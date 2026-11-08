import {test, expect} from 'bun:test';
import {mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, resolve} from 'node:path';
const script=resolve(import.meta.dir,'prepare.sh');
const file='app/src/main/java/app/buddy/assistant/BuildInfo.java';
function fixture(run) {
  const dir=mkdtempSync(join(tmpdir(),'buddy-release-'));
  const cwd=join(dir,'repo');
  mkdirSync(join(cwd,'app/src/main/java/app/buddy/assistant'),{recursive:true});
  const git=(...args)=> {
    const p=Bun.spawnSync(['git',...args],{cwd,env:{...process.env,GIT_CONFIG_GLOBAL:'/dev/null',GIT_CONFIG_NOSYSTEM:'1',GIT_AUTHOR_NAME:'test',GIT_AUTHOR_EMAIL:'test@example.com',GIT_COMMITTER_NAME:'test',GIT_COMMITTER_EMAIL:'test@example.com'}});
    if(p.exitCode) throw new Error(p.stderr.toString());
    return p.stdout.toString().trim();
  };
  const version=(name,code)=> {
    writeFileSync(join(cwd,file),`VERSION = "${name}";\nVERSION_CODE = ${code};\n`);
    git('add',file); git('commit','-qm',name);
  };
  git('init','-q'); version('2.2.26',195); git('tag','v2.2.26'); version('2.2.26-zees.1',196);
  const prepare=(input='')=>Bun.spawnSync(['bash',script],{cwd,env:{...process.env,RELEASE_VERSION:input,GITHUB_ENV:join(dir,'env'),GIT_CONFIG_GLOBAL:'/dev/null',GIT_CONFIG_NOSYSTEM:'1'}});
  try { run({git,version,prepare,read:()=>readFileSync(join(cwd,file),'utf8'),env:()=>readFileSync(join(dir,'env'),'utf8')}); }
  finally {rmSync(dir,{recursive:true,force:true});}
}
test('no input advances source suffix and code, then next tagged release',()=>fixture(({git,prepare,read,env})=>{
  expect(prepare().exitCode).toBe(0);
  expect(read()).toContain('"2.2.26-zees.2"'); expect(read()).toContain('VERSION_CODE = 197');
  expect(env()).toContain('RELEASE_TAG=v2.2.26-zees.2'); expect(git('status','--porcelain')).toBe('');
  git('tag','v2.2.26-zees.2');
  expect(prepare().exitCode).toBe(0); expect(read()).toContain('"2.2.26-zees.3"');
}));
test('higher upstream base and code advance without downgrading',()=>fixture(({git,version,prepare,read})=>{
  const old=git('rev-parse','HEAD'); version('2.3.0',205); git('tag','v2.3.0'); git('reset','--hard',old);
  expect(prepare().exitCode).toBe(0); expect(read()).toContain('"2.3.0-zees.1"'); expect(read()).toContain('VERSION_CODE = 206');
}));
test('explicit new version normalizes optional v',()=>fixture(({prepare,read})=>{
  expect(prepare('v2.3.0').exitCode).toBe(0); expect(read()).toContain('"2.3.0"');
}));
test('invalid, duplicate and downgrade inputs preserve HEAD and tree',()=>fixture(({git,prepare})=>{
  git('tag','-d','v2.2.26');
  const head=git('rev-parse','HEAD');
  for (const input of ['v2.2.26','2.2.26-zees.1','2.2.25-zees.9','2.3.0\nINJECT=yes','2.3.0;false','2.03.0','2.3.0-zees.0']) {
    expect(prepare(input).exitCode).not.toBe(0);
    expect(git('rev-parse','HEAD')).toBe(head); expect(git('status','--porcelain')).toBe('');
  }
}));
