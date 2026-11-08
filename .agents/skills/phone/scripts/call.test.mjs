import {expect,test} from 'bun:test';
import {spawn} from 'node:child_process';

test('phone credentials and requests bypass inherited HTTP proxies',async()=>{
  let directRequests=0,proxyRequests=0;
  const endpoint=Bun.serve({hostname:'127.0.0.1',port:0,async fetch(request){
    directRequests++;
    expect(request.headers.get('authorization')).toBe('Bearer fixture-token');
    const body=await request.json();
    expect(body.method).toBe('tools/call');
    expect(body.params).toEqual({name:'read_screen',arguments:{}});
    return Response.json({jsonrpc:'2.0',id:1,result:{content:[{type:'text',text:'fixture screen'}]}});
  }});
  const proxy=Bun.serve({hostname:'127.0.0.1',port:0,fetch(){
    proxyRequests++;
    return Response.json({jsonrpc:'2.0',id:1,result:{content:[]}});
  }});
  try{
    const proxyUrl=`http://127.0.0.1:${proxy.port}`;
    const child=spawn(process.execPath,[import.meta.dir+'/call.mjs','read_screen','{}'],{
      env:{...process.env,BUDDY_TOKEN:'fixture-token',BUDDY_URL:`http://127.0.0.1:${endpoint.port}/mcp`,HTTP_PROXY:proxyUrl,http_proxy:proxyUrl,ALL_PROXY:proxyUrl,all_proxy:proxyUrl,NO_PROXY:'',no_proxy:''},
      stdio:'ignore',
    });
    const code=await new Promise((resolve,reject)=>{child.on('error',reject);child.on('exit',resolve);});
    expect(code).toBe(0);
    expect(proxyRequests).toBe(0);
    expect(directRequests).toBe(1);
  }finally{endpoint.stop(true);proxy.stop(true);}
});
