import memberFourClient from './memberFourClient'

/** 6.1.2 欠费确认单接口封装；开发环境由 Vite 代理把 /api 转到 8083。 */
const client = memberFourClient
export const pageVouchers = (params, userId) => client.get('/arrears-vouchers', { params, headers: { 'X-User-Id': userId } })
export const getVoucher = (voucherNo, userId) => client.get('/arrears-vouchers/' + voucherNo, { headers: { 'X-User-Id': userId } })
export const printVoucher = (voucherNo, userId) => client.get('/arrears-vouchers/' + voucherNo + '/print', { headers: { 'X-User-Id': userId } })
export const getMyVoucher = (voucherNo, userId) => client.get('/student/arrears-vouchers/' + voucherNo, { headers: { 'X-User-Id': userId } })
