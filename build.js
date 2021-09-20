const path = require("path")
const fs = require("fs")
const process = require("process")
const size = require('human-format');
const config = require("./vite.config.js")
const { build } = require('vite')
const { minify } = require("terser");
const humanFormat = require("human-format");

const outDir = path.resolve(__dirname, "heroku/static")
const terserOptions =  {
  sourceMap: false,
  nameCache: {},
  format: {
    comments: false,
  },
  mangle: {
    properties: {
      debug: false,
      keep_quoted: "strict",
      reserved: ['$classData', 'main', 'toString', 'constructor', 'length', 'call', 'apply', 'NaN', 'Infinity', 'undefined'],
      regex: /^(\$m_|loadHelp|.*__f\d?_|.*__O|.*L\S+_)/,
    }
  }
}

var i = 1
function runTerserOn(fileName, length) {
  process.stdout.write(`Minifying ${i++}/${length}: ${fileName}...`)
  const absolute = path.join(outDir, fileName)
  const original = fs.readFileSync(absolute, "utf8")
  minify(original, terserOptions).then( minified => {
    fs.writeFileSync(absolute, minified.code, "utf8")
    const fromSize = original.length
    const toSize = minified.code.length
    const ratio = (toSize / fromSize) * 100
    process.stdout.write(` ${humanFormat.bytes(fromSize, { prefix: 'Ki' })}  --> ${humanFormat.bytes(toSize, { prefix: 'Ki' })} (${ratio.toFixed(2)}%)\n`)
  })
}

;(async () => {
  const rollupOutput = await build(config)
  const jsChunks = rollupOutput.output.map(chunk => chunk.fileName).filter(fileName => fileName.endsWith(".js"))

  for (const fileName of jsChunks) {
    await runTerserOn(fileName, jsChunks.length)
  }
})()