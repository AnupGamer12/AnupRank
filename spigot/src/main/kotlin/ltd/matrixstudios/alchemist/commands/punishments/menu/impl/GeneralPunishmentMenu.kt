package ltd.matrixstudios.alchemist.commands.punishments.menu.impl

import com.google.common.base.Stopwatch
import ltd.matrixstudios.alchemist.commands.punishments.menu.HistoryMenu
import ltd.matrixstudios.alchemist.models.grant.types.Punishment
import ltd.matrixstudios.alchemist.models.profile.GameProfile
import ltd.matrixstudios.alchemist.punishments.PunishmentType
import ltd.matrixstudios.alchemist.util.Chat
import ltd.matrixstudios.alchemist.util.menu.Button
import ltd.matrixstudios.alchemist.util.menu.buttons.SimpleActionButton
import ltd.matrixstudios.alchemist.util.menu.pagination.PaginatedMenu
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

class GeneralPunishmentMenu(var profile: GameProfile, var punishmentType: PunishmentType, var player: Player): PaginatedMenu(18, player) {

    override fun getHeaderItems(player: Player): MutableMap<Int, Button> {
        val buttons = mutableMapOf<Int, Button>()

        buttons[4] = SimpleActionButton(Material.PAPER, mutableListOf(), "&cGo Back", 0).setBody { player, slot, click ->
            HistoryMenu(profile, player).openMenu()
        }

        return buttons
    }
    override fun getPagesButtons(player: Player): MutableMap<Int, Button> {
        val buttons = hashMapOf<Int, Button>()

        var index = 0
        for (punishment in profile.getPunishments().filter { it.getGrantable() == punishmentType }) {
            buttons[index++] = GeneralPunishmentButton(punishment)
        }

        return buttons
    }

    override fun getTitle(player: Player): String {
        return Chat.format(punishmentType.color + punishmentType.id + "s")
    }
}