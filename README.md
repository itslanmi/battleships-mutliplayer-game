# 🚢 Battleship - The Ultimate Naval Battle! 🎯

Ahoy, Captain! ⚓ Welcome to **Battleship**, the classic strategy game where you engage in an intense duel on the high seas! 🌊 Fire your shots, sink enemy ships, and claim victory! 🏆

---

## 🎮 How the Game Works
Battleship is a **turn-based strategy game** where two players take turns firing at each other’s fleet. The first to **sink all enemy ships** wins the battle! 💥

### 🛠️ Game Mechanics
- You get **two grids**: one for your fleet and one for attacking the enemy. 📍
- Place your ships **manually** with the keyboard. 🚢📌
- Click on the enemy grid to fire your shots! 🎯
- If you hit a ship, the spot will be **marked**! If you miss, it’ll be **indicated** too. ❌✅
- Keep firing until one fleet is **completely destroyed**! 🔥

---

## 📜 Game Rules
### 📏 Battlefield Setup
✅ The board is a **10x10 grid** (default size).  
✅ Ships come in different sizes and shapes (e.g., 1x1, 1x2, 1x3).  
✅ Ships **can’t overlap**.  

### 🎯 Turn-Based Combat
1️⃣ Players take turns clicking a tile on the enemy grid.  
2️⃣ A **hit** is marked, and the ship takes damage.  
3️⃣ A **miss** is also marked, so you know where not to aim again.  
4️⃣ The game continues until **one player loses all their ships**. 🚢💀

---

## 🏗️ Game Elements
### 🌊 The Battlefield
- **Your Board**: Displays your own ships.
- **Enemy Board**: Where you fire shots to **find and sink** enemy ships!

### 🚢 The Fleet
Your armada consists of 5 ships (17 tiles in total): sizes **5, 4, 3, 3 and 2**.

### 🖥️ Controls
- **Placing ships:** arrow keys move the ship, **R** rotates it, **ENTER** confirms it.
- **Firing:** click a tile on the enemy grid.
- **LAN game:** hold **TAB** to look at your own fleet.

---

## 🌐 Playing over LAN (two PCs on the same network)
1. On both PCs: **Settings → turn on "Play over LAN"** → Back.
2. **Play** opens the LAN lobby. Enter your name.
3. One player types a game name and presses **Host**.
4. The other player sees the game in the list (type in the **search box** to filter by game name or host name) and clicks it. If your network blocks discovery, type the host's IP in **Or by IP** and press Join.
5. Each player places their own fleet. The host fires first. A **hit** gives you another shot, a **miss** passes the turn, and the move timer from Settings applies.
6. Each PC only knows its own ships, so there is no peeking.

**Firewall:** allow Java through the Windows firewall on *private* networks (UDP port 45454 for discovery, TCP port 45455 for the game).

## ▶️ Running
```
./gradlew lwjgl3:run
```

⏳ The battle is near... Are you ready to command your fleet? ⚔️🚢

---

🔥 **May the best admiral win!** 🏆
