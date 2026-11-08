import {lstatSync,readFileSync,mkdtempSync,writeFileSync} from 'node:fs';
import {homedir,tmpdir} from 'node:os';
import {join} from 'node:path';

function token(){
  if(process.env.BUDDY_TOKEN)return process.env.BUDDY_TOKEN.replace(/^Bearer\s+/i,'').trim();
  const path=join(process.env.XDG_CONFIG_HOME||join(homedir(),'.config'),'buddy-phone','token');
  const s=lstatSync(path);
  if(!s.isFile()||s.isSymbolicLink()||s.uid!==process.getuid()||![0o400,0o600].includes(s.mode&0o777))throw Error('Buddy token file must be owned by this user, private (0600), and not a symlink.');
  return readFileSync(path,'utf8').trim();
}

async function main(){
  const [name,arg='{}']=process.argv.slice(2);
  if(!name)throw Error('Usage: bun .agents/skills/phone/scripts/call.mjs <initialize|tools/list|tool> [JSON arguments]');
  const url=new URL(process.env.BUDDY_URL||'http://127.0.0.1:8766/mcp');
  if(url.protocol!=='http:'||url.hostname!=='127.0.0.1'||url.username||url.password||url.pathname!=='/mcp'||url.search||url.hash)throw Error('Use a localhost HTTP /mcp tunnel endpoint.');
  const key=token();
  if(!key)throw Error('Buddy token is empty.');
  const method=['initialize','tools/list'].includes(name)?name:'tools/call';
  const params=name==='initialize'?{protocolVersion:'2025-06-18',capabilities:{},clientInfo:{name:'t3-repo-phone',version:'1'}}:name==='tools/list'?{}:{name,arguments:JSON.parse(arg)};
  // This process only talks to the localhost tunnel; inherited proxies must never receive its bearer.
  process.env.NO_PROXY=process.env.no_proxy='127.0.0.1';
  let response;
  try {response=await fetch(url,{method:'POST',headers:{Authorization:'Bearer '+key,'Content-Type':'application/json'},body:JSON.stringify({jsonrpc:'2.0',id:1,method,params}),signal:AbortSignal.timeout(600000)});}
  catch {throw Error('Phone connection unavailable. Ask the operator to reconnect the tunnel.');}
  if(!response.ok)throw Error('Buddy HTTP '+response.status+'. Check connection and private token.');
  const json=await response.json();
  for(const content of json.result?.content||[]){
    if(content.type!=='image')continue;
    const directory=mkdtempSync(join(tmpdir(),'buddy-phone-'));
    const path=join(directory,content.mimeType==='image/jpeg'?'screen.jpg':'screen.png');
    writeFileSync(path,Buffer.from(content.data,'base64'),{mode:0o600});
    content.type='text';content.text='Phone screenshot saved at '+path;
    delete content.data;delete content.mimeType;
  }
  console.log(JSON.stringify(json,null,2));
  if(json.error||json.result?.isError)process.exitCode=1;
}
main().catch(error=>{console.error(error.code==='ENOENT'?'Buddy token unavailable. Supply BUDDY_TOKEN privately or provision ~/.config/buddy-phone/token.':error.message);process.exitCode=1;});
