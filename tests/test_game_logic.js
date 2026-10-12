/**
 * test_game_logic.js
 * Suite de pruebas automatizada para evaluar la lógica del juego en index.html
 */

const fs = require('fs');
const path = require('path');
const assert = require('assert');
const vm = require('vm');

// Cargar index.html
const htmlPath = path.join(__dirname, '..', 'index.html');
const htmlContent = fs.readFileSync(htmlPath, 'utf8');

function createSandbox() {
  const elements = {};

  const sandbox = {
    console: console,
    setTimeout: setTimeout,
    clearTimeout: clearTimeout,
    setInterval: setInterval,
    clearInterval: clearInterval,
    Date: Date,
    Math: Math,
    JSON: JSON,
    Set: Set,
    Array: Array,
    Object: Object,
    String: String,
    Number: Number,
    Boolean: Boolean,
    RegExp: RegExp,
    URL: URL,
    URLSearchParams: URLSearchParams,
    Error: Error,
    Promise: Promise,

    // Mock navigator
    navigator: {
      clipboard: {
        writeText: async (text) => {
          sandbox.lastCopiedText = text;
          return true;
        }
      }
    },

    // Mock localStorage
    localStorageMock: {},
    localStorage: {
      getItem: (key) => sandbox.localStorageMock[key] || null,
      setItem: (key, val) => { sandbox.localStorageMock[key] = String(val); },
      removeItem: (key) => { delete sandbox.localStorageMock[key]; },
      clear: () => { sandbox.localStorageMock = {}; }
    },

    // Mock Document
    document: {
      querySelectorAll: (selector) => {
        if (selector === '.screen') {
          return Object.values(elements).filter(e => e.className && e.className.includes('screen'));
        }
        if (selector === '.off-num' || selector === '.off-mode' || selector === '.num-btn' ||
            selector === '.mode-btn' || selector === '.distance-btn' || selector === '.intensity-btn') {
          return Object.values(elements).filter(e => e.className && e.className.includes(selector.replace('.', '')));
        }
        if (selector.includes('input:checked')) {
          const parentId = selector.split(' ')[0].replace('#', '');
          const parent = elements[parentId];
          return parent ? parent.querySelectorAll('input:checked') : [];
        }
        return [];
      },
      getElementById: (id) => {
        if (!elements[id]) {
          elements[id] = {
            id: id,
            value: '',
            textContent: '',
            innerText: '',
            innerHTML: '',
            style: {},
            classList: {
              classes: new Set(),
              add: function(c) { this.classes.add(c); },
              remove: function(c) { this.classes.delete(c); },
              contains: function(c) { return this.classes.has(c); }
            },
            querySelectorAll: function(sel) {
              if (sel === 'input:checked') {
                return (this.inputs || []).filter(i => i.checked);
              }
              return [];
            },
            querySelector: function(sel) {
              if (sel === '.dialogue-text') {
                return this.dialogueTextEl || this;
              }
              return null;
            },
            addEventListener: function(event, handler) {
              this.listeners = this.listeners || {};
              this.listeners[event] = handler;
            }
          };
        }
        return elements[id];
      },
      addEventListener: () => {}
    },

    alert: (msg) => { sandbox.lastAlert = msg; },
    confirm: () => true,
    location: { reload: () => {} },

    // Mock Audio Context
    AudioContext: class {
      constructor() { this.state = 'running'; }
      resume() { return Promise.resolve(); }
      createOscillator() {
        return {
          type: '',
          frequency: { value: 0 },
          connect: () => {},
          start: () => {},
          stop: () => {}
        };
      }
      createGain() {
        return {
          gain: { value: 0, exponentialRampToValueAtTime: () => {} },
          connect: () => {}
        };
      }
    }
  };

  sandbox.window = sandbox;
  sandbox.global = sandbox;
  sandbox.webkitAudioContext = sandbox.AudioContext;

  return sandbox;
}

// Extraer e interpretar el código JavaScript de index.html
function loadGameContext() {
  const scriptMatch = htmlContent.match(/<script>([\s\S]*?)<\/script>/);
  if (!scriptMatch) {
    throw new Error('No se encontró el bloque <script> en index.html');
  }

  const jsCode = scriptMatch[1];
  const sandbox = createSandbox();

  vm.createContext(sandbox);
  vm.runInContext(jsCode, sandbox);

  return sandbox;
}

// Helper para ejecutar código dentro del VM
function run(code) {
  return vm.runInContext(code, ctx);
}

// SUITE DE PRUEBAS
let totalTests = 0;
let passedTests = 0;

function test(description, fn) {
  totalTests++;
  try {
    fn();
    passedTests++;
    console.log(`  ✓ ${description}`);
  } catch (err) {
    console.error(`  ✗ ${description}`);
    console.error(`    Error: ${err.message}`);
  }
}

console.log('====================================================');
console.log('   EJECUTANDO SUITE DE PRUEBAS DE LÓGICA DE JUEGO  ');
console.log('====================================================\n');

let ctx;
try {
  ctx = loadGameContext();
} catch (e) {
  console.error('Error cargando el script del juego:', e);
  process.exit(1);
}

// 1. Pruebas de Banco de Preguntas y Retos
console.log('1. Banco de Cartas y Preguntas:');

test('Preguntas base y grupales están definidas correctamente', () => {
  const baseQuestions = run('getQuestions()');
  assert(Array.isArray(baseQuestions) && baseQuestions.length > 0, 'Debe haber preguntas disponibles');
  assert(baseQuestions.some(q => q.includes('fantasía')), 'Debe contener preguntas sobre fantasías');
});

test('Retos consideran juguetes seleccionados', () => {
  run("myToys = ['Dado de posiciones sexuales', 'Vibrador clásico']");
  const dares = run('getDares()');
  assert(dares.some(d => typeof d === 'object' && d.toys && d.toys.includes('Dado de posiciones sexuales')),
    'Los retos deben incluir los juguetes configurados');
});

// 2. Pruebas del Personaje Afrodita y Cooldown Inteligente
console.log('\n2. Personaje y Diálogos de Afrodita:');

test('Afrodita genera frases según categoría y modalidad', () => {
  const phrase = run("speakAsAfrodita('bienvenida', { distanceMode: 'separados', type: 'larga' })");
  assert(typeof phrase === 'string' && phrase.length > 0, 'Afrodita debe devolver un diálogo no vacío');
});

test('Sistema de Cooldown limita la frecuencia de diálogos no forzados', () => {
  run("afroditaCooldown.lastSpeakTime = Date.now(); afroditaCooldown.currentInterval = 18000;");

  const phrase = run("speakAsAfroditaWithCooldown('verdad')");
  assert.strictEqual(phrase, null, 'No debe hablar si no ha transcurrido el tiempo de cooldown');
});

test('Diálogos de bienvenida ignoran el cooldown normal', () => {
  run("afroditaCooldown.lastSpeakTime = Date.now();");
  const phrase = run("speakAsAfroditaWithCooldown('bienvenida')");
  assert(typeof phrase === 'string' && phrase.length > 0, 'Categorías forzadas ignoran cooldown');
});

// 3. Lógica del Sistema de Votación y Evaluación de Retos
console.log('\n3. Sistema de Votación (✔ y ✖):');

test('Votación registra aprobaciones y rechazos correctamente', () => {
  run('resetVotingUI()');
  run('voteChallenge(true)'); // Voto Sí
  run('voteChallenge(true)'); // Voto Sí
  run('voteChallenge(false)'); // Voto No

  assert.strictEqual(run('votesYes'), 2, 'Debe haber 2 votos a favor');
  assert.strictEqual(run('votesNo'), 1, 'Debe haber 1 voto en contra');
});

test('Evaluación de votación determina mayoría de rechazo correctamente', () => {
  run("isOffline = true; playerNames = ['Jugador 1', 'Jugador 2', 'Jugador 3']; votesYes = 0; votesNo = 2;");

  let failCalled = false;
  run("onChallengeFailed = () => { window.failCalled = true; }");

  run('evaluateVoting()');
  assert(ctx.failCalled, 'Si la mayoría rechaza, debe llamar a onChallengeFailed');
});

// 4. Persistencia de Estado en LocalStorage
console.log('\n4. Persistencia de Estado (localStorage):');

test('Guardado de estado de juego en localStorage', () => {
  run("isOffline = true; numPlayers = 4; playerNames = ['Ana', 'Carlos', 'Beatriz', 'David']; currentTurn = 1; currentRound = 2;");

  run('saveGameState()');

  const savedRaw = ctx.localStorage.getItem('afrodita_game_state');
  assert(savedRaw !== null, 'Debe guardar el estado en localStorage');

  const savedData = JSON.parse(savedRaw);
  assert.strictEqual(savedData.numPlayers, 4);
  assert.deepStrictEqual(savedData.playerNames, ['Ana', 'Carlos', 'Beatriz', 'David']);
  assert.strictEqual(savedData.currentTurn, 1);
  assert.strictEqual(savedData.currentRound, 2);
});

test('Restauración de estado de juego desde localStorage', () => {
  run('clearGameState()');
  ctx.localStorage.setItem('afrodita_game_state', JSON.stringify({
    isOffline: true,
    numPlayers: 3,
    playerNames: ['X', 'Y', 'Z'],
    currentTurn: 2,
    currentRound: 3,
    activeScreenId: 'game-screen'
  }));

  const restored = run('restoreGameState()');
  assert.strictEqual(restored, true, 'restoreGameState debe retornar true al restaurar');
  assert.strictEqual(run('numPlayers'), 3);
  assert.strictEqual(run('currentTurn'), 2);
  assert.strictEqual(run('currentRound'), 3);
});

// 5. Manejo de Deep Links para Supabase Auth
console.log('\n5. Manejo de Deep Links:');

test('handleDeepLink procesa URLs con tokens de autenticación', async () => {
  run(`
    supabaseApp = {
      auth: {
        setSession: async ({ access_token, refresh_token }) => {
          if (access_token === 'test_access_token' && refresh_token === 'test_refresh_token') {
            window.sessionSet = true;
            return { data: { session: {}, user: {} }, error: null };
          }
          return { data: null, error: new Error('Token inválido') };
        }
      }
    };
  `);

  const deepLinkUrl = 'afrodita://auth/callback#access_token=test_access_token&refresh_token=test_refresh_token&type=signup';
  await run(`handleDeepLink("${deepLinkUrl}")`);

  assert(ctx.sessionSet, 'handleDeepLink debe extraer y establecer los tokens de sesión correctamente');
});

// RESUMEN
console.log('\n====================================================');
console.log(`  RESULTADOS: ${passedTests} de ${totalTests} PRUEBAS PASADAS`);
console.log('====================================================\n');

if (passedTests !== totalTests) {
  process.exit(1);
}
