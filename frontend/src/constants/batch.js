/** 所有业务页面统一使用“批次名称（批次编号）”展示批次身份。 */
export function formatBatchLabel(batch, fallback = '—') {
  const name = String(batch?.batchName || '').trim()
  const code = String(batch?.batchCode || '').trim()
  if (name && code) return `${name}（${code}）`
  return name || code || fallback
}
