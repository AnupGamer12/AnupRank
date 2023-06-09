package ltd.matrixstudios.alchemist.models.profile

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ltd.matrixstudios.alchemist.Alchemist
import ltd.matrixstudios.alchemist.models.chatcolor.ChatColor
import ltd.matrixstudios.alchemist.models.grant.types.Punishment
import ltd.matrixstudios.alchemist.models.grant.types.RankGrant
import ltd.matrixstudios.alchemist.models.profile.notes.ProfileNote
import ltd.matrixstudios.alchemist.models.ranks.Rank
import ltd.matrixstudios.alchemist.models.server.UniqueServer
import ltd.matrixstudios.alchemist.models.sessions.Session
import ltd.matrixstudios.alchemist.models.tags.Tag
import ltd.matrixstudios.alchemist.punishments.PunishmentType
import ltd.matrixstudios.alchemist.service.expirable.PunishmentService
import ltd.matrixstudios.alchemist.service.expirable.RankGrantService
import ltd.matrixstudios.alchemist.service.expirable.TagGrantService
import ltd.matrixstudios.alchemist.service.profiles.ProfileGameService
import ltd.matrixstudios.alchemist.service.ranks.RankService
import ltd.matrixstudios.alchemist.service.session.SessionService
import ltd.matrixstudios.alchemist.service.tags.TagService
import org.bson.Document
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class GameProfile(
    var uuid: UUID,
    var username: String,
    var lowercasedUsername: String,
    var metadata: JsonObject = JsonObject(),
    var ip: String,
    var friends: ArrayList<UUID> = ArrayList(),
    var friendInvites: ArrayList<UUID> = ArrayList(),
    var activeColor: ChatColor? = null,
    var activePrefix: String? = null,
    var permissions: MutableList<String> = ArrayList(),
    var lastSeenAt: Long,
    val notes: MutableList<ProfileNote> = ArrayList(),
) {

    @Transient
    var currentSession: Session? = null

    fun getPunishments(): Collection<Punishment> {
        return PunishmentService.getFromCache(uuid)
    }

    fun getActivePunishments() : Collection<Punishment> {
        return getPunishments().filter { it.expirable.isActive() }
    }

    fun getAltAccounts(): MutableList<GameProfile> {
        val finalAccounts = arrayListOf<GameProfile>()
        val targetDocuments = ProfileGameService.collection.find(Document("ip", ip))


        for (document in targetDocuments) {
            val documentJson = document.toJson()

            val profile = Alchemist.gson.fromJson(documentJson, GameProfile::class.java)

            finalAccounts.add(profile)

        }

        return finalAccounts
    }

    fun createNewSession(server: UniqueServer) : Session
    {
        val session = Session(UUID.randomUUID().toString().substring(0, 4), uuid, mutableMapOf(), System.currentTimeMillis(), 0L)

        session.serversJoined[System.currentTimeMillis()] = server

        SessionService.save(session)
        SessionService.loadIntoCache(this)

        return session
    }

    fun hasActivePrefix(): Boolean {
        return activePrefix != null
    }

    fun hasMetadata(key: String) : Boolean
    {
        return metadata.get(key) != null
    }

    fun getMetadata(key: String) : JsonElement
    {
        return metadata[key]
    }

    fun getActivePrefix(): Tag? {
        val tag = TagService.byId(activePrefix!!) ?: return null

        return tag
    }

    fun canUse(tag: Tag): Boolean {
        return TagGrantService.getValues().get()
            .filter {
                it.target == uuid && it.expirable.isActive()
            }.firstOrNull {
                it.getGrantable()!!.id == tag.id
            } != null
    }

    fun isOnline(): Boolean {
        if (metadata.get("server") == null) return false

        return metadata.get("server").asString != "None"
    }

    fun supplyFriendsAsProfiles(): CompletableFuture<List<GameProfile>> {
        return CompletableFuture.supplyAsync {
            friends.map { ProfileGameService.byId(it) }.filter { Objects.nonNull(it) }.map { it!! }
        }
    }

    fun getActivePunishments(type: PunishmentType): Collection<Punishment> {
        return getPunishments().filter { it.getGrantable() == type && it.expirable.isActive() }
    }

    fun getPermissionsAsList(): MutableList<String> {
        val allPerms = arrayListOf<String>()

        allPerms.addAll(getCurrentRank()!!.permissions)

        val parents = getCurrentRank()!!.parents.map {
            RankService.byId(it)
        }.filter {
            Objects.nonNull(it)
        }

        parents.forEach { rank ->
            rank!!.permissions.forEach {
                if (!allPerms.contains(it)) {
                    allPerms.add(it)
                }
            }
        }


        return allPerms
    }

    fun getPermissions(): HashMap<String?, Boolean?> {
        val returnedPerms = hashMapOf<String?, Boolean?>()
        val allPerms = arrayListOf<String>()

        allPerms.addAll(getCurrentRank()!!.permissions)

        val parents = getCurrentRank()!!.parents.map {
            RankService.byId(it)
        }

        parents.forEach { rank ->
            if (rank != null)
            {
                allPerms.addAll(rank.getAllPermissions())
            }
        }


        allPerms.forEach {
            returnedPerms[it] = true
        }

        for (permission in permissions)
        {
            if (!returnedPerms.containsKey(permission))
            {
                returnedPerms[permission] = true
            }
        }

        return returnedPerms
    }


    fun hasActivePunishment(type: PunishmentType): Boolean {
        return getPunishments().find { it.expirable.isActive() && it.getGrantable() == type } != null
    }

    fun getCurrentGrant(): RankGrant? {
        val filteredRank = RankGrantService.getFromCache(uuid).filter {
            it.expirable.isActive()
        }.sortedBy { it.getGrantable()!!.weight }.reversed().firstOrNull()

        return filteredRank
    }

    fun getCurrentRank(): Rank? {
        var currentGrant: Rank? = RankService.findFirstAvailableDefaultRank()

        val filteredRank = RankGrantService.getFromCache(uuid).filter {
            it.expirable.isActive()
        }.sortedBy { it.getGrantable()!!.weight }.reversed().firstOrNull()

        if (filteredRank == null
            ||
            filteredRank.getGrantable()!!.weight < currentGrant!!.weight
        )
        {
            return currentGrant
        }

        return filteredRank.getGrantable()
    }
}