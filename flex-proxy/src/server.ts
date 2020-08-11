import cookie from 'cookie'
import express from 'express'
import http from 'http'
import { createProxyMiddleware } from 'http-proxy-middleware'

import pathsFromConfig from './parse-config'

const app = express()
const port = 8080
const paths = pathsFromConfig('routes.yaml')

const allowedOrigins = (process.env.ALLOWED_ORIGINS as string).split(',')

app.get('/isAlive', (req, res) => res.send('I\'m alive!'))
app.get('/isReady', (req, res) => res.send('I\'m ready!'))

app.disable('x-powered-by')

interface StringMap {
    [index: string]: string
}

const addHeaders = (proxyReq: http.ClientRequest, req: express.Request) => {
    console.log(`Mottar forespørsel mot ${req.url}`)
    proxyReq.setHeader('x-nav-apiKey', `${process.env.SERVICE_GATEWAY_KEY}`)
    if (req.headers.cookie && !req.headers.Authorization) {
        const parsed = cookie.parse(req.headers.cookie)
        if (parsed && parsed['selvbetjening-idtoken']) {
            proxyReq.setHeader('Authorization', `Bearer ${parsed['selvbetjening-idtoken']}`)
        }
    }
}

app.use((req, res, next) => {
    if (allowedOrigins.includes(req.headers.origin as string)) {
        res.header('Access-Control-Allow-Origin', req.headers.origin)
        res.header('Access-Control-Allow-Credentials', 'true')
    }
    res.header('Access-Control-Allow-Methods', 'DELETE, POST, PUT, GET, OPTIONS')
    res.header('Access-Control-Allow-Headers', 'Content-Type, Accept')
    res.header('Cache-Control', 'no-cache, no-store, must-revalidate')
    next()
})

const addProxy = (method: string, path: string, target: string) => {

    console.log(`Mapper ${method} - ${path} til ${target}`)

    const router = express.Router()

    const pathRewriteKey = `^${path}/`
    const pathRewrite: StringMap = {}
    pathRewrite[pathRewriteKey] = '/'

    if (method === 'GET') {
        router.get(
            '/',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    } else if (method === 'POST') {
        router.post(
            '/',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    } else if (method === 'DELETE') {
        router.delete(
            '/',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    } else if (method === 'PUT') {
        router.put(
            '/',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    } else {
        console.error(`Ukjent HTTP metode ${method}`)
    }

    router.options('/', (req, res) => {
        res.sendStatus(204)
        res.end()
    })

    app.use(path, router)
}

Object.keys(paths).forEach(method => {
    paths[method].forEach(path => addProxy(method, path, `${process.env.SERVICE_GATEWAY_URL}`))
})

app.listen(port, () => console.log(`flex-proxy kjører og lytter på port ${port}`))
