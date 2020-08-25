/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.call.model.MxCallImpl
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.RoomEventSender
import java.util.UUID
import javax.inject.Inject

@SessionScope
internal class ActiveCallHandler @Inject constructor(
        @UserId
        private val userId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val roomEventSender: RoomEventSender
) {

    private val activeCallListLiveData: MutableLiveData<MutableList<MxCall>> by lazy {
        MutableLiveData<MutableList<MxCall>>(mutableListOf())
    }

    fun onNewCall(roomId: String, otherUserId: String, isVideoCall: Boolean): MxCall {
        return MxCallImpl(
                callId = UUID.randomUUID().toString(),
                isOutgoing = true,
                roomId = roomId,
                userId = userId,
                otherUserId = otherUserId,
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                roomEventSender = roomEventSender
        ).also {
            activeCallListLiveData.postValue(activeCallListLiveData.value?.apply { add(it) })
        }
    }

    fun onNewCall(event: Event): MxCall? {
        val content = event.getClearContent().toModel<CallInviteContent>()
        if (content?.callId == null || event.roomId == null || event.senderId == null) {
            return null
        }
        return MxCallImpl(
                callId = content.callId,
                isOutgoing = false,
                roomId = event.roomId,
                userId = userId,
                otherUserId = event.senderId,
                isVideoCall = content.isVideo(),
                localEchoEventFactory = localEchoEventFactory,
                roomEventSender = roomEventSender
        ).also {
            activeCallListLiveData.postValue(activeCallListLiveData.value?.apply { add(it) })
        }
    }

    fun onCallHangup(callId: String) {
        activeCallListLiveData.postValue(activeCallListLiveData.value?.apply { removeAll { it.callId == callId } })
    }

    fun getCallWithId(callId: String): MxCall? {
        return activeCallListLiveData.value?.find { it.callId == callId }
    }

    fun getActiveCallsLiveData(): LiveData<MutableList<MxCall>> = activeCallListLiveData
}
