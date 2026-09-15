package cn.edu.pku.openrunner.feature.auth.data

import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.UserDto
import cn.edu.pku.openrunner.core.session.SessionStore

/**
 * Converts an IAAA access token into a PKU New Youth API session.
 * The IAAA UI/SDK stays outside this repository boundary.
 */
class AuthRepository(private val sessionStore: SessionStore) {
    suspend fun exchangeIaaaToken(iaaaAccessToken: String): UserDto {
        val user = ApiClient.api.exchangeIaaaToken(iaaaAccessToken).requireData()
        val token = user.accessToken
            ?: error("PKU New Youth did not return an access token")
        sessionStore.save(
            userId = user.id,
            token = token,
            userName = user.name,
            department = user.department
        )
        return user
    }
}
