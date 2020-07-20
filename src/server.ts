import cookie from 'cookie'
import express from 'express'
import http from 'http'
import { createProxyMiddleware } from 'http-proxy-middleware'

import pathsFromConfig from './parse-config'

const app = express()
const port = 8080
const paths = pathsFromConfig('./config/routes.yaml')


interface StringMap {
    [index: string]: string
}

const addHeaders = (proxyReq: http.ClientRequest, req: express.Request) => {
    if (req.headers.cookie && !req.headers.Authorization) {
        const parsed = cookie.parse(req.headers.cookie)
        if (parsed && parsed['selvbetjening-idtoken']) {
            proxyReq.setHeader('Authorization', `Bearer ${parsed['selvbetjening-idtoken']}`)
        }
    }
}

const addProxy = (method: string, path: string, target: string) => {
    const router = express.Router()

    const pathRewriteKey = `^${path}`
    const pathRewrite: StringMap = {}
    pathRewrite[pathRewriteKey] = '/'
    if (method === 'GET') {
        router.get(
            '*',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    } else if (method === 'POST') {
        router.post(
            '*',
            createProxyMiddleware({
                target,
                changeOrigin: true,
                pathRewrite,
                onProxyReq: addHeaders,
            }),
        )
    }

    app.use(path, router)
}

app.use(function(req, res, next) {
    res.header('Access-Control-Allow-Origin', req.headers.origin)
    res.header('Access-Control-Allow-Methods', 'DELETE, POST, PUT, GET, OPTIONS')
    res.header('Access-Control-Allow-Credentials', 'true')
    res.header('Access-Control-Allow-Headers', 'Content-Type, Accept')

    if (req.method === 'OPTIONS') {
        res.sendStatus(204)
        res.end()
    } else {
        next()
    }
})

Object.keys(paths).forEach(method => {
    paths[method].forEach(path => addProxy(method, path, path))
})

app.listen(port, () => console.log(`flex-proxy kjører og lytter på port ${port}`))