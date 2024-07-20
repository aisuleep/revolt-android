package chat.revolt.api.routes.server

import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltError
import chat.revolt.api.RevoltHttp
import chat.revolt.api.RevoltJson
import chat.revolt.api.schemas.Member
import chat.revolt.api.schemas.Server
import chat.revolt.api.schemas.User
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

@Serializable
data class FetchMembersResponse(
    val members: List<Member>,
    val users: List<User>
)

suspend fun ackServer(serverId: String) {
    RevoltHttp.put("/servers/$serverId/ack")
}

suspend fun fetchMembers(
    serverId: String,
    includeOffline: Boolean = false,
    pure: Boolean = false
): FetchMembersResponse {
    val response = RevoltHttp.get("/servers/$serverId/members") {
        parameter("exclude_offline", !includeOffline)
    }

    val responseContent = response.bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), responseContent)
        throw Error(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val membersResponse =
        RevoltJson.decodeFromString(FetchMembersResponse.serializer(), responseContent)

    if (pure) {
        return membersResponse
    }

    membersResponse.members.forEach { member ->
        if (!RevoltAPI.members.hasMember(serverId, member.id!!.user)) {
            RevoltAPI.members.setMember(serverId, member)
        }
    }

    membersResponse.users.forEach { user ->
        user.id?.let { RevoltAPI.userCache.putIfAbsent(it, user) }
    }

    return membersResponse
}

suspend fun fetchMember(serverId: String, userId: String, pure: Boolean = false): Member {
    val response = RevoltHttp.get("/servers/$serverId/members/$userId")

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response.bodyAsText())
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    val member = RevoltJson.decodeFromString(Member.serializer(), response.bodyAsText())

    if (!pure) {
        member.id?.let {
            if (!RevoltAPI.members.hasMember(serverId, it.user)) {
                RevoltAPI.members.setMember(serverId, member)
            }
        }
    }

    return member
}

suspend fun leaveOrDeleteServer(serverId: String, leaveSilently: Boolean = false) {
    RevoltHttp.delete("/servers/$serverId") {
        parameter("leave_silently", leaveSilently)
    }
}

suspend fun patchServer(
    serverId: String,
    name: String? = null,
    description: String? = null,
    icon: String? = null,
    banner: String? = null,
    remove: List<String>? = null,
    pure: Boolean = false
) {
    val body = mutableMapOf<String, JsonElement>()

    if (name != null) {
        body["name"] = RevoltJson.encodeToJsonElement(String.serializer(), name)
    }

    if (description != null) {
        body["description"] = RevoltJson.encodeToJsonElement(String.serializer(), description)
    }

    if (icon != null) {
        body["icon"] = RevoltJson.encodeToJsonElement(String.serializer(), icon)
    }

    if (banner != null) {
        body["banner"] = RevoltJson.encodeToJsonElement(String.serializer(), banner)
    }

    if (remove != null) {
        body["remove"] = RevoltJson.encodeToJsonElement(ListSerializer(String.serializer()), remove)
    }


    val response = RevoltHttp.patch("/servers/$serverId") {
        contentType(ContentType.Application.Json)
        setBody(
            RevoltJson.encodeToString(
                MapSerializer(
                    String.serializer(),
                    JsonElement.serializer()
                ),
                body
            )
        )
    }
        .bodyAsText()

    try {
        val error = RevoltJson.decodeFromString(RevoltError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    if (!pure) {
        val server = RevoltJson.decodeFromString(Server.serializer(), response)
        RevoltAPI.serverCache[serverId] = server
    }
}