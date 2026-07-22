import api from './index'

/** 6.1.2 欠费确认单接口封装；开发环境由 Vite 代理把 /api 转到 8083。 */
const client = api
export const pageVouchers = (params) => client.get('/arrears-vouchers', { params })
export const getVoucher = (voucherNo) => client.get('/arrears-vouchers/' + voucherNo)
export const printVoucher = (voucherNo) => client.get('/arrears-vouchers/' + voucherNo + '/print')
export const getMyVoucher = (voucherNo) => client.get('/student/arrears-vouchers/' + voucherNo)
