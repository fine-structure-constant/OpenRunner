package cn.edu.pku.openrunner.feature.tasks.data

import cn.edu.pku.openrunner.core.network.ApiClient
import cn.edu.pku.openrunner.core.network.PkuNewYouthApi
import cn.edu.pku.openrunner.core.network.TaskDto
import cn.edu.pku.openrunner.core.session.SessionStore

class TaskRepository(
    private val sessionStore: SessionStore,
    private val api: PkuNewYouthApi = ApiClient.api
) {
    suspend fun loadVisibleTasks(): List<TaskDto> {
        val userId = sessionStore.userId ?: error("未登录")
        return api.getTasks(userId)
            .requireData()
            .values
            .filter(TaskDto::isVisible)
            .sortedWith(compareBy<TaskDto> { it.activityId == 0 }.thenBy { it.id })
    }
}
