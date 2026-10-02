/**
 * Remix Sludoko - Progressive Web App (PWA) Engine
 */

// --- 1. Service Worker & PWA Install Management ---
let deferredInstallPrompt = null;

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js')
      .then((reg) => console.log('PWA ServiceWorker geregistreerd met scope: ', reg.scope))
      .catch((err) => console.log('ServiceWorker registratiefout: ', err));
  });
}

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
  const installBtn = document.getElementById('install-btn');
  if (installBtn) {
    installBtn.classList.add('visible');
  }
});

function handleInstallClick() {
  if (deferredInstallPrompt) {
    deferredInstallPrompt.prompt();
    deferredInstallPrompt.userChoice.then((choiceResult) => {
      if (choiceResult.outcome === 'accepted') {
        console.log('Gebruiker heeft de PWA installatie geaccepteerd.');
      }
      deferredInstallPrompt = null;
      const installBtn = document.getElementById('install-btn');
      if (installBtn) installBtn.classList.remove('visible');
    });
  } else {
    // Show manual install info dialog
    showModal('install-info-modal');
  }
}

// --- 2. Web Audio Synthesizer (No external sound files required) ---
class SoundManager {
  constructor() {
    this.ctx = null;
    this.enabled = true;
  }

  init() {
    if (!this.ctx && (window.AudioContext || window.webkitAudioContext)) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      this.ctx = new AudioCtx();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playTone(freq, type = 'sine', duration = 0.08, gainVal = 0.15) {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = type;
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      gain.gain.setValueAtTime(gainVal, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch (e) {
      // Audio might be blocked by browser policy until user gesture
    }
  }

  click() { this.playTone(600, 'sine', 0.04, 0.08); }
  number() { this.playTone(880, 'sine', 0.08, 0.12); }
  note() { this.playTone(1100, 'triangle', 0.05, 0.08); }
  erase() { this.playTone(350, 'sawtooth', 0.07, 0.1); }
  error() {
    this.playTone(220, 'sawtooth', 0.15, 0.2);
    setTimeout(() => this.playTone(180, 'sawtooth', 0.2, 0.2), 80);
  }
  victory() {
    const notes = [523.25, 659.25, 783.99, 1046.50];
    notes.forEach((freq, idx) => {
      setTimeout(() => this.playTone(freq, 'sine', 0.25, 0.2), idx * 120);
    });
  }
}

const sounds = new SoundManager();

// --- 3. Speech Synthesizer (Dutch nl-NL) ---
class DutchSpeech {
  constructor() {
    this.enabled = true;
  }
  speak(text) {
    if (!this.enabled || !('speechSynthesis' in window)) return;
    try {
      window.speechSynthesis.cancel();
      const utter = new SpeechSynthesisUtterance(text);
      utter.lang = 'nl-NL';
      utter.rate = 1.1;
      window.speechSynthesis.speak(utter);
    } catch (e) {}
  }
}

const speech = new DutchSpeech();

// --- 4. Sudoku Puzzle Generator & Solver ---
const DIFFICULTY_CONFIG = {
  EASY: { clues: 40, name: 'Makkelijk' },
  MEDIUM: { clues: 33, name: 'Gemiddeld' },
  HARD: { clues: 28, name: 'Moeilijk' },
  EXPERT: { clues: 24, name: 'Expert' }
};

class SudokuEngine {
  static createEmptyGrid() {
    return Array.from({ length: 9 }, () => Array(9).fill(0));
  }

  static cloneGrid(grid) {
    return grid.map(row => [...row]);
  }

  static isValid(grid, row, col, num) {
    for (let c = 0; c < 9; c++) {
      if (grid[row][c] === num) return false;
    }
    for (let r = 0; r < 9; r++) {
      if (grid[r][col] === num) return false;
    }
    const startRow = Math.floor(row / 3) * 3;
    const startCol = Math.floor(col / 3) * 3;
    for (let r = 0; r < 3; r++) {
      for (let c = 0; c < 3; c++) {
        if (grid[startRow + r][startCol + c] === num) return false;
      }
    }
    return true;
  }

  static fillBox(grid, startRow, startCol) {
    const nums = [1, 2, 3, 4, 5, 6, 7, 8, 9].sort(() => Math.random() - 0.5);
    let idx = 0;
    for (let r = 0; r < 3; r++) {
      for (let c = 0; c < 3; c++) {
        grid[startRow + r][startCol + c] = nums[idx++];
      }
    }
  }

  static solve(grid) {
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (grid[r][c] === 0) {
          const nums = [1, 2, 3, 4, 5, 6, 7, 8, 9].sort(() => Math.random() - 0.5);
          for (const num of nums) {
            if (this.isValid(grid, r, c, num)) {
              grid[r][c] = num;
              if (this.solve(grid)) return true;
              grid[r][c] = 0;
            }
          }
          return false;
        }
      }
    }
    return true;
  }

  static countSolutions(grid, limit = 2) {
    let count = 0;
    function backtrack() {
      for (let r = 0; r < 9; r++) {
        for (let c = 0; c < 9; c++) {
          if (grid[r][c] === 0) {
            for (let num = 1; num <= 9; num++) {
              if (SudokuEngine.isValid(grid, r, c, num)) {
                grid[r][c] = num;
                backtrack();
                grid[r][c] = 0;
                if (count >= limit) return;
              }
            }
            return;
          }
        }
      }
      count++;
    }
    backtrack();
    return count;
  }

  static generate(difficulty = 'MEDIUM') {
    const solution = this.createEmptyGrid();
    for (let i = 0; i < 9; i += 3) {
      this.fillBox(solution, i, i);
    }
    this.solve(solution);

    const puzzle = this.cloneGrid(solution);
    const targetClues = (DIFFICULTY_CONFIG[difficulty] || DIFFICULTY_CONFIG.MEDIUM).clues;

    const positions = [];
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        positions.push([r, c]);
      }
    }
    positions.sort(() => Math.random() - 0.5);

    let cluesRemaining = 81;
    for (const [r, c] of positions) {
      if (cluesRemaining <= targetClues) break;
      const backup = puzzle[r][c];
      puzzle[r][c] = 0;

      const testGrid = this.cloneGrid(puzzle);
      if (this.countSolutions(testGrid, 2) !== 1) {
        puzzle[r][c] = backup;
      } else {
        cluesRemaining--;
      }
    }

    return { puzzle, solution, difficulty };
  }
}

// --- 5. Game State & Logic ---
class SludokoGame {
  constructor() {
    this.difficulty = 'MEDIUM';
    this.initialGrid = [];
    this.currentGrid = [];
    this.solution = [];
    this.notes = Array.from({ length: 9 }, () => Array.from({ length: 9 }, () => new Set()));
    this.selectedCell = { row: -1, col: -1 };
    this.notesMode = false;
    this.timer = 0;
    this.timerInterval = null;
    this.isPaused = false;
    this.mistakes = 0;
    this.maxMistakes = 3;
    this.mistakeLimitEnabled = true;
    this.undoStack = [];
    this.stats = this.loadStats();
    this.settings = this.loadSettings();
    this.applySettings();
  }

  loadStats() {
    const raw = localStorage.getItem('sludoko_stats');
    if (raw) {
      try { return JSON.parse(raw); } catch (e) {}
    }
    return {
      gamesPlayed: 0,
      gamesWon: 0,
      bestTimes: { EASY: null, MEDIUM: null, HARD: null, EXPERT: null }
    };
  }

  saveStats() {
    localStorage.setItem('sludoko_stats', JSON.stringify(this.stats));
  }

  loadSettings() {
    const raw = localStorage.getItem('sludoko_settings');
    if (raw) {
      try { return JSON.parse(raw); } catch (e) {}
    }
    return {
      theme: 'dark',
      sound: true,
      speech: true,
      mistakeLimit: true,
      autoClearNotes: true
    };
  }

  saveSettings() {
    localStorage.setItem('sludoko_settings', JSON.stringify(this.settings));
    this.applySettings();
  }

  applySettings() {
    document.documentElement.setAttribute('data-theme', this.settings.theme);
    sounds.enabled = this.settings.sound;
    speech.enabled = this.settings.speech;
    this.mistakeLimitEnabled = this.settings.mistakeLimit;
    const limitDisplay = document.getElementById('mistakes-max');
    if (limitDisplay) {
      limitDisplay.textContent = this.mistakeLimitEnabled ? '/3' : '';
    }
  }

  startNewGame(difficulty = this.difficulty) {
    this.difficulty = difficulty;
    const gen = SudokuEngine.generate(difficulty);
    this.initialGrid = gen.puzzle;
    this.solution = gen.solution;
    this.currentGrid = SudokuEngine.cloneGrid(this.initialGrid);
    this.notes = Array.from({ length: 9 }, () => Array.from({ length: 9 }, () => new Set()));
    this.selectedCell = { row: 4, col: 4 }; // Select middle cell initially
    this.mistakes = 0;
    this.undoStack = [];
    this.isPaused = false;
    this.timer = 0;
    this.updateTimerDisplay();

    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      if (!this.isPaused) {
        this.timer++;
        this.updateTimerDisplay();
      }
    }, 1000);

    this.renderBoard();
    this.renderKeypad();
    this.updateStatusBar();
    this.saveGameState();

    const diffEl = document.getElementById('difficulty-select');
    if (diffEl) diffEl.value = this.difficulty;
  }

  saveGameState() {
    try {
      const state = {
        difficulty: this.difficulty,
        initialGrid: this.initialGrid,
        solution: this.solution,
        currentGrid: this.currentGrid,
        notes: this.notes.map(r => r.map(set => Array.from(set))),
        timer: this.timer,
        mistakes: this.mistakes
      };
      localStorage.setItem('sludoko_active_game', JSON.stringify(state));
    } catch (e) {}
  }

  restoreGameState() {
    const raw = localStorage.getItem('sludoko_active_game');
    if (raw) {
      try {
        const state = JSON.parse(raw);
        if (state.initialGrid && state.solution) {
          this.difficulty = state.difficulty || 'MEDIUM';
          this.initialGrid = state.initialGrid;
          this.solution = state.solution;
          this.currentGrid = state.currentGrid;
          this.notes = state.notes.map(r => r.map(arr => new Set(arr)));
          this.timer = state.timer || 0;
          this.mistakes = state.mistakes || 0;
          this.selectedCell = { row: 4, col: 4 };
          this.isPaused = false;

          if (this.timerInterval) clearInterval(this.timerInterval);
          this.timerInterval = setInterval(() => {
            if (!this.isPaused) {
              this.timer++;
              this.updateTimerDisplay();
            }
          }, 1000);

          this.renderBoard();
          this.renderKeypad();
          this.updateStatusBar();
          return true;
        }
      } catch (e) {}
    }
    return false;
  }

  updateTimerDisplay() {
    const m = Math.floor(this.timer / 60).toString().padStart(2, '0');
    const s = (this.timer % 60).toString().padStart(2, '0');
    const timerEl = document.getElementById('timer-text');
    if (timerEl) timerEl.textContent = `${m}:${s}`;
  }

  togglePause() {
    this.isPaused = !this.isPaused;
    const overlay = document.getElementById('pause-overlay');
    const pauseIcon = document.getElementById('pause-icon');
    if (overlay) overlay.style.display = this.isPaused ? 'flex' : 'none';
    if (pauseIcon) pauseIcon.textContent = this.isPaused ? '▶' : '⏸';
  }

  selectCell(row, col) {
    if (this.isPaused) return;
    this.selectedCell = { row, col };
    sounds.click();
    this.highlightBoard();

    // Spraakondersteuning
    const val = this.currentGrid[row][col];
    const valText = val !== 0 ? `Cijfer ${val}` : 'Leeg';
    speech.speak(`Rij ${row + 1}, Kolom ${col + 1}. ${valText}`);
  }

  highlightBoard() {
    const cells = document.querySelectorAll('.sudoku-cell');
    const { row: selR, col: selC } = this.selectedCell;
    const selectedVal = (selR >= 0 && selC >= 0) ? this.currentGrid[selR][selC] : 0;

    cells.forEach(cell => {
      const r = parseInt(cell.dataset.row, 10);
      const c = parseInt(cell.dataset.col, 10);
      const val = this.currentGrid[r][c];

      cell.classList.remove('selected', 'same-related', 'same-value');

      if (r === selR && c === selC) {
        cell.classList.add('selected');
      } else if (selR >= 0 && selC >= 0) {
        const inSameBox = Math.floor(r / 3) === Math.floor(selR / 3) && Math.floor(c / 3) === Math.floor(selC / 3);
        if (r === selR || c === selC || inSameBox) {
          cell.classList.add('same-related');
        }
        if (selectedVal !== 0 && val === selectedVal) {
          cell.classList.add('same-value');
        }
      }
    });
  }

  inputNumber(num) {
    if (this.isPaused) return;
    const { row, col } = this.selectedCell;
    if (row < 0 || col < 0) return;
    if (this.initialGrid[row][col] !== 0) return; // Cannot edit original puzzle clues

    if (this.notesMode) {
      // Toggle note
      const currentNotes = new Set(this.notes[row][col]);
      if (currentNotes.has(num)) {
        currentNotes.delete(num);
      } else {
        currentNotes.add(num);
      }
      this.undoStack.push({
        type: 'note',
        row, col,
        oldNotes: new Set(this.notes[row][col]),
        newNotes: new Set(currentNotes)
      });
      this.notes[row][col] = currentNotes;
      sounds.note();
      this.renderCell(row, col);
      this.saveGameState();
      return;
    }

    // Direct placement mode
    const oldVal = this.currentGrid[row][col];
    const oldNotes = new Set(this.notes[row][col]);
    if (oldVal === num) return; // Same number

    const isCorrect = this.solution[row][col] === num;

    this.undoStack.push({
      type: 'value',
      row, col,
      oldVal,
      newVal: num,
      oldNotes
    });

    if (isCorrect) {
      this.currentGrid[row][col] = num;
      this.notes[row][col].clear();

      // Auto-clear notes in same row/col/box if enabled
      if (this.settings.autoClearNotes) {
        for (let i = 0; i < 9; i++) {
          this.notes[row][i].delete(num);
          this.notes[i][col].delete(num);
        }
        const bR = Math.floor(row / 3) * 3;
        const bC = Math.floor(col / 3) * 3;
        for (let r = 0; r < 3; r++) {
          for (let c = 0; c < 3; c++) {
            this.notes[bR + r][bC + c].delete(num);
          }
        }
      }

      sounds.number();
      this.renderBoard();
      this.renderKeypad();
      this.saveGameState();
      this.checkVictory();
    } else {
      this.mistakes++;
      this.updateStatusBar();
      sounds.error();
      const cellEl = document.querySelector(`.sudoku-cell[data-row="${row}"][data-col="${col}"]`);
      if (cellEl) {
        cellEl.classList.add('error');
        setTimeout(() => cellEl.classList.remove('error'), 600);
      }

      if (this.mistakeLimitEnabled && this.mistakes >= this.maxMistakes) {
        this.gameOver();
      }
    }
  }

  erase() {
    if (this.isPaused) return;
    const { row, col } = this.selectedCell;
    if (row < 0 || col < 0) return;
    if (this.initialGrid[row][col] !== 0) return;

    const oldVal = this.currentGrid[row][col];
    const oldNotes = new Set(this.notes[row][col]);
    if (oldVal === 0 && oldNotes.size === 0) return;

    this.undoStack.push({
      type: 'erase',
      row, col,
      oldVal,
      oldNotes
    });

    this.currentGrid[row][col] = 0;
    this.notes[row][col].clear();
    sounds.erase();
    this.renderCell(row, col);
    this.renderKeypad();
    this.highlightBoard();
    this.saveGameState();
  }

  undo() {
    if (this.isPaused || this.undoStack.length === 0) return;
    const lastAction = this.undoStack.pop();
    const { row, col } = lastAction;

    if (lastAction.type === 'value' || lastAction.type === 'erase') {
      this.currentGrid[row][col] = lastAction.oldVal;
      this.notes[row][col] = lastAction.oldNotes;
    } else if (lastAction.type === 'note') {
      this.notes[row][col] = lastAction.oldNotes;
    }

    this.selectedCell = { row, col };
    sounds.click();
    this.renderBoard();
    this.renderKeypad();
    this.saveGameState();
  }

  provideHint() {
    if (this.isPaused) return;
    // Find empty cell
    const emptyCells = [];
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (this.currentGrid[r][c] === 0) {
          emptyCells.push({ r, c });
        }
      }
    }

    if (emptyCells.length === 0) return;

    // Pick selected cell if empty, otherwise first empty
    let target = emptyCells.find(cell => cell.r === this.selectedCell.row && cell.c === this.selectedCell.col);
    if (!target) target = emptyCells[Math.floor(Math.random() * emptyCells.length)];

    const correctVal = this.solution[target.r][target.c];
    this.selectedCell = { row: target.r, col: target.c };
    this.currentGrid[target.r][target.c] = correctVal;
    this.notes[target.r][target.c].clear();

    sounds.victory();
    this.renderBoard();
    this.renderKeypad();
    this.saveGameState();
    this.checkVictory();

    showModal('hint-modal', `Tip toegepast op rij ${target.r + 1}, kolom ${target.c + 1}: ${correctVal}`);
  }

  autoFillNotes() {
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (this.currentGrid[r][c] === 0) {
          this.notes[r][c].clear();
          for (let n = 1; n <= 9; n++) {
            if (SudokuEngine.isValid(this.currentGrid, r, c, n)) {
              this.notes[r][c].add(n);
            }
          }
        }
      }
    }
    sounds.note();
    this.renderBoard();
    this.saveGameState();
  }

  checkVictory() {
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (this.currentGrid[r][c] !== this.solution[r][c]) {
          return false;
        }
      }
    }

    // Won the game!
    if (this.timerInterval) clearInterval(this.timerInterval);
    sounds.victory();
    this.triggerConfetti();

    this.stats.gamesPlayed++;
    this.stats.gamesWon++;
    const best = this.stats.bestTimes[this.difficulty];
    if (!best || this.timer < best) {
      this.stats.bestTimes[this.difficulty] = this.timer;
    }
    this.saveStats();
    localStorage.removeItem('sludoko_active_game');

    const m = Math.floor(this.timer / 60).toString().padStart(2, '0');
    const s = (this.timer % 60).toString().padStart(2, '0');
    document.getElementById('victory-time').textContent = `${m}:${s}`;
    document.getElementById('victory-diff').textContent = (DIFFICULTY_CONFIG[this.difficulty] || {}).name || this.difficulty;
    showModal('victory-modal');
    return true;
  }

  gameOver() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.stats.gamesPlayed++;
    this.saveStats();
    localStorage.removeItem('sludoko_active_game');
    showModal('gameover-modal');
  }

  triggerConfetti() {
    const container = document.getElementById('confetti-box');
    if (!container) return;
    container.innerHTML = '';
    const colors = ['#6366f1', '#06b6d4', '#10b981', '#f59e0b', '#ec4899'];
    for (let i = 0; i < 60; i++) {
      const piece = document.createElement('div');
      piece.style.position = 'absolute';
      piece.style.width = `${Math.random() * 8 + 6}px`;
      piece.style.height = `${Math.random() * 12 + 6}px`;
      piece.style.backgroundColor = colors[Math.floor(Math.random() * colors.length)];
      piece.style.left = `${Math.random() * 100}%`;
      piece.style.top = '-10px';
      piece.style.opacity = Math.random() + 0.5;
      piece.style.transform = `rotate(${Math.random() * 360}deg)`;
      piece.style.transition = `top ${Math.random() * 2 + 2}s cubic-bezier(0.25, 0.46, 0.45, 0.94), transform 3s ease`;
      container.appendChild(piece);

      setTimeout(() => {
        piece.style.top = '105%';
        piece.style.transform = `rotate(${Math.random() * 720}deg) scale(0.5)`;
      }, 50);
    }
    setTimeout(() => { container.innerHTML = ''; }, 4500);
  }

  updateStatusBar() {
    const mistakesEl = document.getElementById('mistakes-count');
    if (mistakesEl) mistakesEl.textContent = this.mistakes;
  }

  renderBoard() {
    const gridEl = document.getElementById('sudoku-grid');
    if (!gridEl) return;
    gridEl.innerHTML = '';

    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        const cell = document.createElement('div');
        cell.className = 'sudoku-cell';
        cell.dataset.row = r;
        cell.dataset.col = c;

        this.renderCellContent(cell, r, c);
        cell.addEventListener('click', () => this.selectCell(r, c));
        gridEl.appendChild(cell);
      }
    }
    this.highlightBoard();
  }

  renderCell(row, col) {
    const cellEl = document.querySelector(`.sudoku-cell[data-row="${row}"][data-col="${col}"]`);
    if (cellEl) {
      this.renderCellContent(cellEl, row, col);
    }
  }

  renderCellContent(cellEl, row, col) {
    cellEl.className = 'sudoku-cell';
    const initialVal = this.initialGrid[row][col];
    const currentVal = this.currentGrid[row][col];
    const cellNotes = this.notes[row][col];

    if (initialVal !== 0) {
      cellEl.classList.add('initial');
      cellEl.textContent = initialVal;
    } else if (currentVal !== 0) {
      cellEl.classList.add('user-filled');
      cellEl.textContent = currentVal;
    } else if (cellNotes.size > 0) {
      cellEl.innerHTML = '';
      const notesGrid = document.createElement('div');
      notesGrid.className = 'notes-grid';
      for (let n = 1; n <= 9; n++) {
        const noteItem = document.createElement('div');
        noteItem.className = 'note-item';
        noteItem.textContent = cellNotes.has(n) ? n : '';
        notesGrid.appendChild(noteItem);
      }
      cellEl.appendChild(notesGrid);
    } else {
      cellEl.textContent = '';
    }
  }

  renderKeypad() {
    // Count placed numbers
    const counts = Array(10).fill(0);
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        const v = this.currentGrid[r][c];
        if (v > 0) counts[v]++;
      }
    }

    for (let n = 1; n <= 9; n++) {
      const keyBtn = document.getElementById(`key-${n}`);
      const countEl = document.getElementById(`count-${n}`);
      const remaining = 9 - counts[n];
      if (countEl) countEl.textContent = remaining > 0 ? remaining : '✓';
      if (keyBtn) {
        if (remaining <= 0) {
          keyBtn.classList.add('completed');
        } else {
          keyBtn.classList.remove('completed');
        }
      }
    }
  }
}

// --- 6. UI Helpers & Modals ---
function showModal(id, customText = '') {
  const modal = document.getElementById(id);
  if (modal) {
    if (customText) {
      const textEl = modal.querySelector('.modal-message');
      if (textEl) textEl.textContent = customText;
    }
    modal.classList.add('active');
  }
}

function hideModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

// Global Game Instance
let game = null;

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  game = new SludokoGame();
  const restored = game.restoreGameState();
  if (!restored) {
    game.startNewGame('MEDIUM');
  }

  // Setup Event Listeners
  document.getElementById('difficulty-select')?.addEventListener('change', (e) => {
    game.startNewGame(e.target.value);
  });

  document.getElementById('pause-btn')?.addEventListener('click', () => game.togglePause());
  document.getElementById('resume-btn')?.addEventListener('click', () => game.togglePause());
  document.getElementById('restart-btn')?.addEventListener('click', () => {
    if (confirm('Weet je zeker dat je deze puzzel opnieuw wilt starten?')) {
      game.startNewGame(game.difficulty);
    }
  });

  // Action Buttons
  document.getElementById('undo-btn')?.addEventListener('click', () => game.undo());
  document.getElementById('erase-btn')?.addEventListener('click', () => game.erase());
  document.getElementById('hint-btn')?.addEventListener('click', () => game.provideHint());
  document.getElementById('magic-btn')?.addEventListener('click', () => {
    if (confirm('Wil je automatisch alle mogelijke notities invullen?')) {
      game.autoFillNotes();
    }
  });

  const notesToggleBtn = document.getElementById('notes-toggle-btn');
  notesToggleBtn?.addEventListener('click', () => {
    game.notesMode = !game.notesMode;
    notesToggleBtn.classList.toggle('active', game.notesMode);
    sounds.click();
  });

  // Keypad 1-9
  for (let n = 1; n <= 9; n++) {
    document.getElementById(`key-${n}`)?.addEventListener('click', () => {
      game.inputNumber(n);
    });
  }

  // Keyboard navigation & number input
  window.addEventListener('keydown', (e) => {
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space'].includes(e.code)) {
      e.preventDefault();
    }
    const { row, col } = game.selectedCell;
    if (e.key >= '1' && e.key <= '9') {
      game.inputNumber(parseInt(e.key, 10));
    } else if (e.key === 'Backspace' || e.key === 'Delete') {
      game.erase();
    } else if (e.key === 'n' || e.key === 'N') {
      game.notesMode = !game.notesMode;
      notesToggleBtn?.classList.toggle('active', game.notesMode);
    } else if (e.key === 'u' || e.key === 'U' || (e.ctrlKey && e.key === 'z')) {
      game.undo();
    } else if (e.key === 'ArrowUp' && row > 0) {
      game.selectCell(row - 1, col);
    } else if (e.key === 'ArrowDown' && row < 8) {
      game.selectCell(row + 1, col);
    } else if (e.key === 'ArrowLeft' && col > 0) {
      game.selectCell(row, col - 1);
    } else if (e.key === 'ArrowRight' && col < 8) {
      game.selectCell(row, col + 1);
    }
  });

  // Header Dialog buttons
  document.getElementById('stats-btn')?.addEventListener('click', () => {
    const stats = game.stats;
    document.getElementById('stat-played').textContent = stats.gamesPlayed;
    document.getElementById('stat-won').textContent = stats.gamesWon;
    const rate = stats.gamesPlayed > 0 ? Math.round((stats.gamesWon / stats.gamesPlayed) * 100) : 0;
    document.getElementById('stat-rate').textContent = `${rate}%`;

    const formatTime = (sec) => {
      if (!sec) return '--:--';
      const m = Math.floor(sec / 60).toString().padStart(2, '0');
      const s = (sec % 60).toString().padStart(2, '0');
      return `${m}:${s}`;
    };
    document.getElementById('best-easy').textContent = formatTime(stats.bestTimes.EASY);
    document.getElementById('best-medium').textContent = formatTime(stats.bestTimes.MEDIUM);
    document.getElementById('best-hard').textContent = formatTime(stats.bestTimes.HARD);
    document.getElementById('best-expert').textContent = formatTime(stats.bestTimes.EXPERT);
    showModal('stats-modal');
  });

  document.getElementById('settings-btn')?.addEventListener('click', () => {
    document.getElementById('setting-dark-theme').checked = game.settings.theme === 'dark';
    document.getElementById('setting-sound').checked = game.settings.sound;
    document.getElementById('setting-speech').checked = game.settings.speech;
    document.getElementById('setting-mistakes').checked = game.settings.mistakeLimit;
    showModal('settings-modal');
  });

  document.getElementById('help-btn')?.addEventListener('click', () => {
    showModal('help-modal');
  });

  document.getElementById('install-btn')?.addEventListener('click', () => {
    handleInstallClick();
  });

  // Setting Changes
  document.getElementById('setting-dark-theme')?.addEventListener('change', (e) => {
    game.settings.theme = e.target.checked ? 'dark' : 'light';
    game.saveSettings();
  });
  document.getElementById('setting-sound')?.addEventListener('change', (e) => {
    game.settings.sound = e.target.checked;
    game.saveSettings();
  });
  document.getElementById('setting-speech')?.addEventListener('change', (e) => {
    game.settings.speech = e.target.checked;
    game.saveSettings();
  });
  document.getElementById('setting-mistakes')?.addEventListener('change', (e) => {
    game.settings.mistakeLimit = e.target.checked;
    game.saveSettings();
  });

  // Modal Close buttons
  document.querySelectorAll('.close-modal-trigger').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const modal = e.target.closest('.modal-overlay');
      if (modal) modal.classList.remove('active');
    });
  });

  // Dialog new games
  document.getElementById('victory-new-game')?.addEventListener('click', () => {
    hideModal('victory-modal');
    game.startNewGame(game.difficulty);
  });
  document.getElementById('gameover-new-game')?.addEventListener('click', () => {
    hideModal('gameover-modal');
    game.startNewGame(game.difficulty);
  });
});
