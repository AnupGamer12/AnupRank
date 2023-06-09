package ltd.matrixstudios.alchemist.punishment.packets

import ltd.matrixstudios.alchemist.AlchemistSpigotPlugin
import ltd.matrixstudios.alchemist.api.AlchemistAPI
import ltd.matrixstudios.alchemist.punishments.PunishmentType
import ltd.matrixstudios.alchemist.redis.RedisPacket
import ltd.matrixstudios.alchemist.util.Chat
import ltd.matrixstudios.alchemist.util.TimeUtil
import org.bukkit.Bukkit
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.*

class PunishmentExecutePacket(
    var punishmentType: PunishmentType,
    var target: UUID,
    var reason: String
) : RedisPacket("punishment-execute") {

    override fun action() {
        val player = Bukkit.getPlayer(target)

        if (player != null) {
            if (punishmentType == PunishmentType.BLACKLIST) {
                AlchemistSpigotPlugin.instance.config.getStringList("blacklist-message")
                    .map { it.replace("<reason>", reason) }.forEach { player.sendMessage(Chat.format(it)) }
            } else if (punishmentType == PunishmentType.BAN) {
                AlchemistSpigotPlugin.instance.config.getStringList("ban-message")
                    .map { it.replace("<reason>", reason) }.forEach { player.sendMessage(Chat.format(it)) }
            } else if (punishmentType == PunishmentType.MUTE) {
                AlchemistSpigotPlugin.instance.config.getStringList("mute-message")
                    .map { it.replace("<reason>", reason) }.forEach { player.sendMessage(Chat.format(it)) }
            } else if (punishmentType == PunishmentType.WARN) {
                AlchemistSpigotPlugin.instance.config.getStringList("warn-message")
                    .map { it.replace("<reason>", reason) }.forEach { player.sendMessage(Chat.format(it)) }
            }

            if (punishmentType == PunishmentType.BAN) {

                val profile = AlchemistAPI.syncFindProfile(target) ?: return
                val punishment = profile.getActivePunishments(PunishmentType.BAN).firstOrNull()
                val msgs = AlchemistSpigotPlugin.instance.config.getStringList("banned-join")

                msgs.replaceAll { it.replace("<reason>", punishment!!.reason) }
                msgs.replaceAll {
                    it.replace(
                        "<expires>",
                        if (punishment!!.expirable.duration == Long.MAX_VALUE) "Never" else TimeUtil.formatDuration(
                            punishment.expirable.addedAt + punishment.expirable.duration - System.currentTimeMillis()
                        )
                    )
                }

                player.kickPlayer(msgs.map { Chat.format(it) }.joinToString("\n"))
            } else if (punishmentType == PunishmentType.BLACKLIST) {
                val profile = AlchemistAPI.syncFindProfile(target) ?: return
                val punishments = profile.getActivePunishments(PunishmentType.BLACKLIST).toMutableList()
                punishments.addAll(profile.getActivePunishments(PunishmentType.BAN))

                val punishment = profile.getActivePunishments(PunishmentType.BLACKLIST).firstOrNull()
                val msgs = AlchemistSpigotPlugin.instance.config.getStringList("blacklisted-join")

                msgs.replaceAll { it.replace("<reason>", punishment!!.reason) }
                msgs.replaceAll {
                    it.replace(
                        "<expires>",
                        if (punishment!!.expirable.duration == Long.MAX_VALUE) "Never" else TimeUtil.formatDuration(
                            punishment.expirable.addedAt + punishment.expirable.duration - System.currentTimeMillis()
                        )
                    )
                }

                player.kickPlayer(msgs.map { Chat.format(it) }.joinToString("\n"))
            }
        }
    }
}