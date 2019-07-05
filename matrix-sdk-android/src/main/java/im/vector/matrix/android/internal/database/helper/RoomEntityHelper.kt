/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.database.helper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.fastContains
import im.vector.matrix.android.internal.extensions.assertIsManaged


internal fun RoomEntity.deleteOnCascade(chunkEntity: ChunkEntity) {
    chunks.remove(chunkEntity)
    chunkEntity.deleteOnCascade()
}

internal fun RoomEntity.addOrUpdate(chunkEntity: ChunkEntity) {
    if (!chunks.contains(chunkEntity)) {
        chunks.add(chunkEntity)
    }
}

internal fun RoomEntity.addStateEvents(stateEvents: List<Event>,
                                       stateIndex: Int = Int.MIN_VALUE,
                                       filterDuplicates: Boolean = false,
                                       isUnlinked: Boolean = false) {
    assertIsManaged()

    stateEvents.forEach { event ->
        if (event.eventId == null || (filterDuplicates && fastContains(event.eventId))) {
            return@forEach
        }
        val eventEntity = event.toEntity(roomId).apply {
            this.stateIndex = stateIndex
            this.isUnlinked = isUnlinked
            this.sendState = SendState.SYNCED
        }
        untimelinedStateEvents.add(0, eventEntity)
    }
}

internal fun RoomEntity.addSendingEvent(event: Event) {
    assertIsManaged()
    val eventEntity = event.toEntity(roomId).apply {
        this.sendState = SendState.UNSENT
    }
    val timelineEventEntity = TimelineEventEntity().also {
        it.root = eventEntity
        it.eventId = event.eventId ?: ""
        it.roomId = roomId
    }
    sendingTimelineEvents.add(0, timelineEventEntity)
}
