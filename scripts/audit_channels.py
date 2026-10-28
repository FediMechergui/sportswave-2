from pathlib import Path
import urllib.request as u, urllib.error as e, urllib.parse as p
import json,base64,concurrent.futures as cf,time,threading,collections
ROOT=Path(__file__).resolve().parents[1]
PREFIX=b'c!xZj+N9&G@Ev@vw'
def get(url,headers=None,limit=1048576):
 req=u.Request(url,headers=headers or {})
 with u.urlopen(req,timeout=12) as r:return r.read(limit),r.headers,r.geturl()
def api(route):
 b,h,_=get('http://def001.ycnapi.com/api/'+route,{'User-Agent':'okhttp/4.12.0','Accept':'application/json'})
 k=PREFIX+h['t'].encode();return json.loads(bytes(v^k[i%len(k)] for i,v in enumerate(base64.b64decode(b))))
def media(url,headers,depth=0):
 b,h,final=get(url,headers,262144)
 if b.lstrip().startswith(b'#EXTM3U'):
  lines=b.decode('utf-8-sig').splitlines()
  links=[x.strip() for x in lines if x.strip() and not x.startswith('#')]
  if not links:return 'empty_playlist'
  if any(x.startswith('#EXT-X-STREAM-INF') for x in lines):
   if depth>=3:return 'playlist_depth'
   return media(p.urljoin(final,links[0]),headers,depth+1)
  key=next((x for x in lines if x.startswith('#EXT-X-KEY:') and 'METHOD=NONE' not in x),None)
  if key:
   import re
   m=re.search(r'URI="([^"]+)"',key)
   if m:
    key_bytes,_,_=get(p.urljoin(final,m[1]),headers,1024)
    if re.search(r'(?:^|,)METHOD=AES-128(?:,|$)',key.split(':',1)[1]) and len(key_bytes)!=16:return 'unsupported_aes128_key_length'
  segment,_,_=get(p.urljoin(final,links[-2] if len(links)>1 else links[0]),headers,4096)
  if segment.startswith((b'\x89PNG\r\n\x1a\n',b'\xff\xd8\xff',b'GIF87a',b'GIF89a')) or (segment.startswith(b'RIFF') and segment[8:12]==b'WEBP'):return 'wrapped_image_segment'
  return 'hls_segment_ok' if segment and not segment.lstrip().lower().startswith((b'<html',b'<!doctype')) else 'invalid_segment'
 if b.lstrip().startswith(b'<') and b'<MPD' in b:return 'dash_manifest_only'
 if b and not b.lstrip().lower().startswith((b'<html',b'<!doctype')) and ('video/' in h.get('Content-Type','') or b[:1]==b'G' or b'ftyp' in b[:64]):return 'media_bytes_ok'
 return 'not_native_media'
def probe(item):
 row=dict(item);row['checkedUtc']=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime())
 try:
  streams=api('channel/'+item['id']).get('data',[]);row['streams']=[]
  for s in streams:
   result={'mode':s.get('url_type',1),'drm':bool(s.get('drm'))}
   if result['drm']:result['status']='drm_required'
   elif result['mode'] not in (1,3,'1','3'):result['status']='unsupported_mode'
   else:
    headers=dict(s.get('headers') or {});headers['User-Agent']=s.get('user_agent') or headers.get('User-Agent') or 'SportsWave/1.0'
    if s.get('referer'):headers['Referer']=s['referer']
    try:result['status']=media(s['url'],headers)
    except e.HTTPError as ex:result['status']='http_'+str(ex.code)
    except Exception as ex:result['status']=type(ex).__name__
   if result['status'] in ('unsupported_mode','drm_required'):
    headers=dict(s.get('headers') or {});headers['User-Agent']=s.get('user_agent') or headers.get('User-Agent') or 'SportsWave/1.0'
    if s.get('referer'):headers['Referer']=s['referer']
    try:
     page,_,_=get(s['url'],headers,4096);result['endpoint_check']='http_200_html' if b'<html' in page.lower() or b'<!doctype html' in page.lower() else 'http_200_bytes'
    except e.HTTPError as ex:result['endpoint_check']='http_'+str(ex.code)
    except Exception as ex:result['endpoint_check']=type(ex).__name__
   row['streams'].append(result)
  row['status']='available' if any(x['status'] in ('hls_segment_ok','media_bytes_ok') for x in row['streams']) else ('no_streams' if not streams else 'failed')
 except e.HTTPError as ex:row['status']='api_http_'+str(ex.code)
 except Exception as ex:row['status']='api_'+type(ex).__name__
 return row
def catalog():
 pending=list(api('categories')['data']);result=[];seen=set()
 while pending:
  category=pending.pop(0)
  if category['id'] in seen:continue
  seen.add(category['id'])
  if category.get('child_count',0):
   for child in api('categories/'+str(category['id']))['data']:
    child['name']=category['name']+' / '+child['name'];pending.append(child)
  else:result.append({'package':category,'channels':api('categories/'+str(category['id'])+'/channels')['data']})
 return result
if __name__=='__main__':
 import argparse
 parser=argparse.ArgumentParser(description='Audit every catalog record without saving signed media URLs. Network delivery checks, not decoded-video tests.')
 parser.add_argument('--catalog',type=Path,help='Optional captured catalog metadata; default fetches the current complete catalog')
 parser.add_argument('--resume',action='store_true',help='Resume the existing report instead of starting a fresh audit')
 parser.add_argument('--package-prefix',default='',help='Only audit leaf packages whose names start with this prefix')
 parser.add_argument('--output',type=Path,default=ROOT/'artifacts/channel-audit.jsonl',help='Sanitized JSONL report destination')
 args=parser.parse_args()
 catalog=json.loads(args.catalog.read_text(encoding='utf-8')) if args.catalog else catalog()
 catalog=[group for group in catalog if group['package']['name'].startswith(args.package_prefix)]
 if not catalog:raise ValueError('No matching catalog packages')
 (ROOT/'artifacts').mkdir(exist_ok=True)
 items=[{'id':str(c['id']),'name':c['name'],'package':g['package']['name']} for g in catalog for c in g['channels']]
 output=args.output;output.parent.mkdir(parents=True,exist_ok=True);done={}
 if args.resume and output.exists():
  for line in output.read_text().splitlines():
   r=json.loads(line);done[r['id']]=r
 with output.open('a' if args.resume else 'w',encoding='utf-8') as out,cf.ThreadPoolExecutor(max_workers=4) as pool:
  futures=[pool.submit(probe,x) for x in items if x['id'] not in done]
  for f in cf.as_completed(futures):
   row=f.result();done[row['id']]=row;out.write(json.dumps(row,ensure_ascii=True)+'\n');out.flush()
   if len(done)%20==0:print(len(done),'/',len(items),dict(collections.Counter(x['status'] for x in done.values())),flush=True)
 print('COMPLETE',len(done),dict(collections.Counter(x['status'] for x in done.values())),flush=True)
