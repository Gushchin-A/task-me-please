import {copyFile, mkdir} from 'node:fs/promises'
import {dirname, resolve} from 'node:path'

const sourceRoot = resolve('node_modules/@primer/primitives/dist/css')
const targetRoot = resolve('src/main/resources/static/css/primer')
const files = [
  'primitives.css',
  'base/motion/motion.css',
  'base/size/size.css',
  'base/size/z-index.css',
  'base/typography/typography.css',
  'functional/motion/motion.css',
  'functional/size/border.css',
  'functional/size/breakpoints.css',
  'functional/size/radius.css',
  'functional/size/size-coarse.css',
  'functional/size/size-fine.css',
  'functional/size/size.css',
  'functional/size/z-index.css',
  'functional/spacing/space.css',
  'functional/typography/typography.css',
  'functional/themes/light.css',
  'functional/themes/dark.css',
]

for (const file of files) {
  const target = resolve(targetRoot, file)
  await mkdir(dirname(target), {recursive: true})
  await copyFile(resolve(sourceRoot, file), target)
}
