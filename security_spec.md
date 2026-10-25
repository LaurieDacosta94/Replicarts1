# Security Specification — Crown & Dominion: Rise of Ages 3D

## 1. Data Invariants
1. **Default Deny**: All unmatched document paths are unconditionally denied (`allow read, write: if false;`).
2. **Authentication Invariant**: Every read and write operation across `/match_lobbies/{lobbyId}` and `/cloud_modpacks/{modpackId}` requires `request.auth != null`.
3. **Ownership & Identity Integrity**:
   - On creation of `/match_lobbies/{lobbyId}` or `/cloud_modpacks/{modpackId}`, `incoming().userId == request.auth.uid`.
   - On update, `incoming().userId == existing().userId` and `incoming().createdAt == existing().createdAt`.
   - Only the owner (`existing().userId == request.auth.uid`) can delete or modify core settings of a lobby or cloud modpack.
4. **Terminal State Locking**: Once a `MatchLobby` reaches `status == 'completed'`, no further updates are permitted.
5. **Query Alignment**:
   - `/match_lobbies`: `allow list` requires `existing().userId == request.auth.uid || existing().visibility == 'public'`.
   - `/cloud_modpacks`: `allow list` requires `existing().userId == request.auth.uid`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Lobby Read**: Unauthenticated client reading `/match_lobbies` -> Rejected.
2. **Identity Spoofing on Lobby Create**: Authenticated user `alice` creates a lobby with `userId: "bob"` -> Rejected.
3. **Ghost Field Injection (Shadow Update)**: Client injects `isAdmin: true` into `/match_lobbies/{id}` -> Rejected by `hasOnly()`.
4. **Oversized String DoS**: Client writes a 10,000-char `lobbyTitle` -> Rejected by `.size() <= 80`.
5. **Invalid Document ID Poisoning**: Client uses non-alphanumeric characters in `{lobbyId}` -> Rejected by `isValidId()`.
6. **Terminal State Mutation**: Host attempts to update a lobby whose `status` is already `'completed'` -> Rejected.
7. **Immutable Timestamp Tampering**: Host attempts to alter `createdAt` during an update -> Rejected.
8. **Future Timestamp Injection**: Client submits a `createdAt` timestamp in the future (`> request.time`) -> Rejected.
9. **Unfiltered Query Scraping**: Client queries `/cloud_modpacks` without `.where("userId", "==", uid)` -> Rejected by `allow list`.
10. **Cross-User Private Modpack Read**: User `alice` attempts to `get` or `update` user `bob`'s `/cloud_modpacks/{id}` -> Rejected.
11. **Out-of-Bounds Player Count**: Client creates a lobby with `maxPlayers: 99` or `currentPlayers: 0` -> Rejected.
12. **Type Poisoning on Update**: Client updates `currentPlayers` with a String `"four"` instead of `int` -> Rejected by `isValidMatchLobby()`.
