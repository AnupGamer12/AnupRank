package ltd.matrixstudios.alchemist.service.session

import io.github.nosequel.data.DataStoreType
import ltd.matrixstudios.alchemist.Alchemist
import ltd.matrixstudios.alchemist.models.profile.GameProfile
import ltd.matrixstudios.alchemist.models.ranks.Rank
import ltd.matrixstudios.alchemist.models.server.UniqueServer
import ltd.matrixstudios.alchemist.models.sessions.Session
import org.bson.Document
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

object SessionService  {

    var handler = Alchemist.dataHandler.createStoreType<String, Session>(DataStoreType.MONGO)
    private val rawCollection = Alchemist.MongoConnectionPool.getCollection("session")

    var cache = hashMapOf<UUID, List<Session>>()

    fun save(session: Session) {
        handler.storeAsync(session.randomId, session)
    }


    fun loadIntoCache(profile: GameProfile)
    {
        val filter = Document("player", profile.uuid.toString())

        val documents = rawCollection.find(filter)
        val sessions = mutableListOf<Session>()

        for (document in documents)
        {
            sessions.add(Alchemist.gson.fromJson(document.toJson(), Session::class.java))
        }

        cache[profile.uuid] = sessions
    }
}