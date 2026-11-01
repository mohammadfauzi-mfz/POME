// POME Monitor - backend Google Apps Script. Tempel di Extensions > Apps Script pada Google Sheet baru.
// Jalankan setup() sekali, lalu Deploy > Web app (Execute as: Me, Access: Anyone).
const SH = {
  Pompa: ['id', 'nama'],
  Blok: ['id', 'luas', 'pompa'],
  Input: ['id', 'tgl', 'pompa', 'hm0', 'hm1', 'vol', 'oleh', 'lat', 'lng', 'akurasi', 'waktu'],
  Alokasi: ['id', 'tgl', 'blok', 'rotasi', 'vol', 'oleh', 'lat', 'lng', 'akurasi', 'waktu', 'riwayat'],
  User: ['username', 'hash', 'role']
};
const PERM = { Pompa: ['PKS', 'Admin'], Blok: ['Kebun', 'Admin'], Input: ['PKS', 'Admin'], Alokasi: ['Kebun', 'Admin'], User: ['Admin'] };

function setup() {
  const ss = SpreadsheetApp.getActive();
  Object.keys(SH).forEach(k => {
    const s = ss.getSheetByName(k) || ss.insertSheet(k);
    if (s.getLastRow() == 0) s.appendRow(SH[k]);
    ensureCols(k);
    s.getRange(1, 1, s.getMaxRows(), s.getLastColumn()).setNumberFormat('@');
  });
  if (rows('User').length == 0) sh('User').appendRow(['admin', hash('admin123'), 'Admin']);
  if (rows('Pompa').length == 0) [['p1', 'Pompa 1'], ['p2', 'Pompa 2'], ['p3', 'Pompa 3']].forEach(r => sh('Pompa').appendRow(r));
  if (!PropertiesService.getScriptProperties().getProperty('KEY'))
    PropertiesService.getScriptProperties().setProperty('KEY', Utilities.getUuid());
}
const sh = n => SpreadsheetApp.getActive().getSheetByName(n);
const hash = p => Utilities.base64Encode(Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, p));
// Tambah kolom baru (mis. lat/lng) ke sheet lama otomatis, tanpa menghapus data.
function ensureCols(n) {
  const s = sh(n), lc = Math.max(s.getLastColumn(), 1), h = s.getRange(1, 1, 1, lc).getValues()[0].map(String);
  SH[n].forEach(c => {
    if (h.indexOf(c) < 0) {
      h.push(c);
      s.getRange(1, h.length).setValue(c);
      s.getRange(1, h.length, s.getMaxRows(), 1).setNumberFormat('@');
    }
  });
}
function rows(n) {
  ensureCols(n);
  const v = sh(n).getDataRange().getValues(), h = v.shift();
  const f = x => x instanceof Date ? Utilities.formatDate(x, Session.getScriptTimeZone(), 'yyyy-MM-dd') : String(x);
  return v.map(r => Object.fromEntries(h.map((k, i) => [k, f(r[i])])));
}
const tok = u => hash(u.username + '|' + u.hash + '|' + PropertiesService.getScriptProperties().getProperty('KEY'));
const out = o => ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(20000);
  try { return out(handle(JSON.parse(e.postData.contents))); }
  catch (err) { return out({ ok: false, err: String(err.message || err) }); }
  finally { lock.releaseLock(); }
}

function handle(q) {
  if (q.action == 'login') {
    const u = rows('User').find(x => x.username == q.username && x.hash == hash(q.password || ''));
    if (!u) throw new Error('Username atau password salah');
    return { ok: true, username: u.username, role: u.role, token: u.username + ':' + tok(u) };
  }
  const t = String(q.token || ''), un = t.split(':')[0];
  const me = rows('User').find(x => x.username == un);
  if (!me || un + ':' + tok(me) != t) throw new Error('Sesi habis, login ulang');
  if (q.action == 'all') return { ok: true, Pompa: rows('Pompa'), Blok: rows('Blok'), Input: rows('Input'), Alokasi: rows('Alokasi') };
  if (q.action == 'users') { need('User', me); return { ok: true, users: rows('User').map(x => ({ username: x.username, role: x.role })) }; }
  if (q.action == 'save') { need(q.t, me); save(q.t, q.row, me); return { ok: true }; }
  if (q.action == 'batch') {
    const by = {};
    (q.items || []).forEach(it => { need(it.t, me); (by[it.t] = by[it.t] || []).push(it.row); });
    Object.keys(by).forEach(k => saveMany(k, by[k], me));
    return { ok: true };
  }
  if (q.action == 'del') { need(q.t, me); del(q.t, q.id); return { ok: true }; }
  throw new Error('Aksi tidak dikenal');
}
function need(t, me) { if (!SH[t] || PERM[t].indexOf(me.role) < 0) throw new Error('Tidak punya akses'); }

function save(t, row, me) {
  ensureCols(t);
  const s = sh(t), k = t == 'User' ? 'username' : 'id', v = s.getDataRange().getValues(), h = v[0], ki = h.indexOf(k);
  if (t == 'User' && row.password) row.hash = hash(row.password);
  if (h.indexOf('oleh') >= 0 && !row.oleh) row.oleh = me.username;
  const arr = h.map(c => row[c] === undefined ? '' : row[c]);
  for (let i = 1; i < v.length; i++) {
    if (String(v[i][ki]) == String(row[k])) {
      if (t == 'User' && !row.password) arr[h.indexOf('hash')] = v[i][h.indexOf('hash')];
      s.getRange(i + 1, 1, 1, h.length).setValues([arr]);
      return;
    }
  }
  s.appendRow(arr);
}
function del(t, id) {
  const s = sh(t), k = t == 'User' ? 'username' : 'id', v = s.getDataRange().getValues(), ki = v[0].indexOf(k);
  for (let i = v.length - 1; i >= 1; i--) if (String(v[i][ki]) == String(id)) s.deleteRow(i + 1);
}

// Simpan banyak baris sekaligus (impor): baca sheet sekali, update yang ada, tambah yang baru.
function saveMany(t, list, me) {
  ensureCols(t);
  const s = sh(t), k = t == 'User' ? 'username' : 'id', v = s.getDataRange().getValues(), h = v[0], ki = h.indexOf(k), idx = {}, add = [];
  for (let i = 1; i < v.length; i++) idx[String(v[i][ki])] = i;
  list.forEach(row => {
    if (h.indexOf('oleh') >= 0 && !row.oleh) row.oleh = me.username;
    const arr = h.map(c => row[c] === undefined ? '' : row[c]), i = idx[String(row[k])];
    if (i !== undefined) s.getRange(i + 1, 1, 1, h.length).setValues([arr]);
    else add.push(arr);
  });
  if (add.length) s.getRange(s.getLastRow() + 1, 1, add.length, h.length).setValues(add);
}
