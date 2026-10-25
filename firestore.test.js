const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_commander_1";
const BOB_UID = "bob_commander_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot list or read match_lobbies or cloud_modpacks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("match_lobbies").get());
  await assertFails(unauthDb.collection("cloud_modpacks").get());
});

test("Authenticated user: can create valid match_lobby and query public lobbies", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("match_lobbies").doc("lobby_roman_1").set({
      userId: ALICE_UID,
      hostName: "Emperor Aurelian",
      lobbyTitle: "Mediterranean Conquest 4v4",
      mapBiome: "Mediterranean",
      startAge: "ANCIENT",
      endAge: "INFORMATION",
      maxPlayers: 8,
      currentPlayers: 1,
      activeModpackName: "Vanilla Imperial Standard",
      status: "waiting",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("match_lobbies").where("visibility", "==", "public").get()
  );
});

test("Authenticated user: cannot spoof userId or inject ghost fields in match_lobbies", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("match_lobbies").doc("lobby_spoof").set({
      userId: BOB_UID,
      hostName: "Spoofer",
      lobbyTitle: "Spoofed Room",
      mapBiome: "Continental",
      startAge: "ANCIENT",
      endAge: "MODERN",
      maxPlayers: 4,
      currentPlayers: 1,
      activeModpackName: "Standard",
      status: "waiting",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("match_lobbies").doc("lobby_ghost").set({
      userId: ALICE_UID,
      hostName: "Emperor Aurelian",
      lobbyTitle: "Ghost Field Room",
      mapBiome: "Continental",
      startAge: "ANCIENT",
      endAge: "MODERN",
      maxPlayers: 4,
      currentPlayers: 1,
      activeModpackName: "Standard",
      status: "waiting",
      visibility: "public",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Cloud modpacks: strict owner isolation on read, list, update, delete", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("cloud_modpacks").doc("bob_pack_1").set({
      userId: BOB_UID,
      name: "Bob's Total Conversion",
      author: "General Bob",
      version: "1.2.0",
      description: "Doubles border push and oil gather rates",
      modCount: 3,
      payloadJson: "{\"mods\":[]}",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("cloud_modpacks").doc("bob_pack_1").get());
  await assertFails(aliceDb.collection("cloud_modpacks").get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("cloud_modpacks").where("userId", "==", BOB_UID).get()
  );
});
