package ltd.matrixstudios.alchemist.redis.impl.caches

import ltd.matrixstudios.alchemist.models.profile.GameProfile
import ltd.matrixstudios.alchemist.redis.RedisPacket
import ltd.matrixstudios.alchemist.service.expirable.RankGrantService
import ltd.matrixstudios.alchemist.service.profiles.ProfileGameService
import java.util.*

class UpdateGrantCacheRequest(val target: UUID) : RedisPacket("grant-sync-update") {

    override fun action() {
        RankGrantService.recalculateUUID(target)
    }
}