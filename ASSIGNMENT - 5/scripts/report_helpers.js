// Builds report/CSA09_Assignment4_Report.docx. The uploaded assignment pages come FIRST, then the answers.
const fs = require('fs');
const path = require('path');
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun, HeadingLevel, AlignmentType, WidthType,
  ShadingType, BorderStyle, LevelFormat, Footer, PageNumber, SectionType } = require('docx');

const ROOT = path.resolve(__dirname, '..');
const rd = f => fs.readFileSync(path.join(ROOT, f), 'utf8');
function csv(file) {
  const lines = rd(file).trim().split('\n'); const head = lines[0].split(',');
  return lines.slice(1).map(l => { const c = l.split(','); const o = {}; head.forEach((h, i) => o[h] = c[i]); return o; });
}
const f1 = x => Number(x).toFixed(1), f2 = x => Number(x).toFixed(2), f3 = x => Number(x).toFixed(3);
const fmt = x => Number(x).toLocaleString('en-US');

const runs = (text, opts = {}) => text.split(/(\*\*[^*]+\*\*)/).filter(s => s.length).map(s =>
  s.startsWith('**') ? new TextRun({ text: s.slice(2, -2), bold: true, ...opts }) : new TextRun({ text: s, ...opts }));
const P = (text, opts = {}) => new Paragraph({ children: runs(text), spacing: { after: 110, line: 276 }, ...opts });
const H1 = t => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)], pageBreakBefore: true });
const H2 = t => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const H3 = t => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const B = text => new Paragraph({ numbering: { reference: 'bul', level: 0 }, children: runs(text), spacing: { after: 60, line: 276 } });
const caption = t => new Paragraph({ children: [new TextRun({ text: t, italics: true, size: 18 })], alignment: AlignmentType.CENTER, spacing: { after: 160 } });
const gap = () => new Paragraph({ children: [], spacing: { after: 100 } });
const codeLines = (txt, size = 15, fill = 'F2F2F2') => txt.replace(/\s+$/, '').split('\n').map(l => new Paragraph({
  children: [new TextRun({ text: l.length ? l.replace(/\t/g, '    ') : ' ', font: 'Courier New', size })],
  shading: { type: ShadingType.CLEAR, fill }, spacing: { after: 0, line: 220 }, indent: { left: 100, right: 100 } }));
const code = txt => codeLines(txt.replace(/^\n/, ''));
// a source file from the repository, with a small title line
const codeFile = (file, note) => [new Paragraph({ children: [new TextRun({ text: file + (note ? '   -   ' + note : ''), bold: true, size: 18, color: '2F5496' })], spacing: { before: 140, after: 40 }, keepNext: true }), ...codeLines(rd(file))];
// a captured console output
const outFile = (file, title) => [new Paragraph({ children: [new TextRun({ text: title || file, bold: true, size: 18, color: '375623' })], spacing: { before: 120, after: 40 }, keepNext: true }), ...codeLines(rd(file), 14, 'E8F0E0')];

const border = { style: BorderStyle.SINGLE, size: 4, color: 'AAAAAA' };
const borders = { top: border, bottom: border, left: border, right: border };
function table(headers, rows, widths, opts = {}) {
  const total = widths.reduce((a, b) => a + b, 0), size = opts.size || 17;
  const mk = (txt, w, head, align) => new TableCell({ borders, width: { size: w, type: WidthType.DXA },
    shading: head ? { type: ShadingType.CLEAR, fill: 'D9E2F3' } : undefined, margins: { top: 50, bottom: 50, left: 80, right: 80 },
    children: String(txt).split('\n').map(line => new Paragraph({ alignment: align, children: runs(line, { size, bold: head }) })) });
  const trs = [new TableRow({ tableHeader: true, children: headers.map((h, i) => mk(h, widths[i], true, AlignmentType.LEFT)) })];
  rows.forEach(r => trs.push(new TableRow({ cantSplit: true, children: r.map((c, i) => mk(c, widths[i], false, (i === 0 || opts.leftAll) ? AlignmentType.LEFT : AlignmentType.RIGHT)) })));
  return new Table({ width: { size: total, type: WidthType.DXA }, columnWidths: widths, rows: trs });
}
function img(file, maxW = 540, maxH = 700) {
  const buf = fs.readFileSync(path.join(ROOT, file));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  const sc = Math.min(maxW / w, maxH / h, 1.6);
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 80, after: 40 }, keepNext: true,
    children: [new ImageRun({ type: 'png', data: buf, transformation: { width: Math.round(w * sc), height: Math.round(h * sc) }, altText: { title: file, description: file, name: file } })] });
}
const fig = (file, maxW, maxH, cap) => [img(file, maxW, maxH), caption(cap)];
const wordCount = s => s.trim().split(/\s+/).length;

