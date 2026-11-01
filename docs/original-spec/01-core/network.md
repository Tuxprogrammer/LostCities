# Network channel and packets

Upstream registers one channel with two packet types. At the pin both handlers are empty and nothing sends either packet: the channel is inert. On 1.12 the same pair carried the dimension's profile name to the client (used for profile-driven fog).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `network/PacketHandler` (channel creation and registration; the channel field is private with no getter). `network/PacketRequestProfile`, `network/PacketReturnProfileToClient`. |
| 2 | **Inputs** | Packet buffers only. |
| 3 | **Numbers** | See [Packets](#packets). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Not applicable. The message-ID counter is a static that starts at 12 and is advanced at registration, so IDs depend on registration running exactly once. |
| 6 | **Data formats** | Packet fields below. No NBT. |
| 7 | **Output** | None at the pin. |
| 8 | **Relationships** | Registered from `ModSetup.init` ([lifecycle.md](lifecycle.md)). No sender anywhere in the code. |
| 9 | **1.20 dependencies** | Forge `SimpleChannel` with a protocol-version string and client/server acceptance predicates; `FriendlyByteBuf`; `NetworkEvent.Context.enqueueWork`; dimension keys sent as resource locations. |
| 10 | **Bugs** | `CORE-36` |

## Packets

| Item | Value |
|---|---|
| Channel | `lostcities:lostcities` |
| Protocol version | `"1.0"` |
| Client accepts server version | any (`s -> true`) |
| Server accepts client version | any (`s -> true`) |
| First message ID | 12 (`PacketHandler.ID`); a second unused counter `packetId` starts at 0 |

| ID | Packet | Intended direction | Fields, in order | Handler at the pin |
|---|---|---|---|---|
| 12 | `PacketRequestProfile` | client → server | `dimension`: resource location (dimension key) | Enqueues an empty task, marks handled. A commented-out body would answer with the profile name. |
| 13 | `PacketReturnProfileToClient` | server → client | `dimension`: resource location; `profile`: UTF-8 string, read with a 32767-character limit | Enqueues an empty task, marks handled. A commented-out body would store the profile on the client. |

Validation: none needed at the pin, because both handlers ignore their content. Neither handler checks the sender or the direction. A message arriving in the "wrong" direction is processed the same way (nothing happens).

## Behavior detail

1. (both) At common setup, create the channel with the version and the two accept-anything predicates.
2. (both) Register `PacketRequestProfile` as message 12 and `PacketReturnProfileToClient` as message 13, each with its encoder, decoder constructor and handler.
3. (both) On receipt, decode the fields, enqueue a no-op on the main thread, and mark the packet handled.

## On 1.12

`upstream/1.12` uses `SimpleNetworkWrapper` (available in 1.7.10) with the same two packets carrying an integer dimension ID. `WorldTypeTools` sends `PacketRequestProfile` from the client, and the server replies with `PacketReturnProfileToClient`; the client stores the profile and uses its `FOG_RED`, `FOG_GREEN`, `FOG_BLUE` and `FOG_DENSITY` in fog events. 1.20 keeps those fog handlers only as comments (`09-client-ui`).

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
