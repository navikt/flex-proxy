import fs from 'fs'
import YAML from 'yaml'

const loadFile = (filename: string) => {
    const file = fs.readFileSync(filename, 'utf8')
    return YAML.parse(file)
}

const verifyBaseKeys = (config: Record<string, unknown>): boolean => {
    const acceptedMethods = [ 'POST', 'GET' ]
    return Object.keys(config).every(key => acceptedMethods.includes(key))
}

const configToPaths = (object: any, root = '') => {
    const result: string[] = []
    const getPaths = (data: any, root: string) => {
        if (data && typeof data == 'object') {
            if (Array.isArray(data)) {
                for (let i = 0; i < data.length; i++) {
                    getPaths(data[i], root + '/' + data[i])
                }
            } else {
                for (const p in data) {
                    getPaths(data[p], root + '/' + p)
                }
            }
        } else {
            result.push(root)
        }
    }
    getPaths(object, root)


    return result

}

const pathsFromConfig = (filename: string): Record<string, string[]> => {
    const config = loadFile(filename)
    if (verifyBaseKeys(config)) {
        const methods = Object.keys(config)
        const paths: Record<string, string[]> = {}
        methods.forEach(key => { 
            paths[key] = configToPaths(config[key])
        })
        return paths
    }
    return {}
}

export default pathsFromConfig