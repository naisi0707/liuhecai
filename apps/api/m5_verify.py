import json
import urllib.request
import urllib.error

BASE = 'http://127.0.0.1:8080'

def call(method, path, body=None, headers=None):
    h = {'Content-Type': 'application/json'}
    if headers:
        h.update(headers)
    data = None if body is None else json.dumps(body).encode('utf-8')
    req = urllib.request.Request(BASE + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode('utf-8'))

agent = call('POST', '/api/agent/auth/login', {'username': 'agent_a', 'password': 'agent123'})
assert agent['code'] == 0, agent
token = agent['data']['token']
auth = {'Authorization': f'Bearer {token}', 'X-Forwarded-Host': 'lbw.local'}

before = call('GET', '/api/tenant/current', headers={'X-Forwarded-Host': 'lbw.local'})
print('before color', before['data'].get('primaryColor'))

updated = call('PUT', '/api/agent/site-config', {
    'name': '刘伯温论坛·换肤',
    'announcement': 'M5 换肤公告',
    'kefuWechat': 'm5_wechat',
    'kefuQq': '123456',
    'primaryColor': '#1565c0',
    'fontFamily': 'SimHei',
    'logoUrl': '',
    'adBanner': '今日特惠广告位'
}, auth)
print('update', updated['code'], updated['data']['primaryColor'], updated['data']['name'])
assert updated['code'] == 0
assert updated['data']['primaryColor'] == '#1565c0'

after = call('GET', '/api/tenant/current', headers={'X-Forwarded-Host': 'lbw.local'})
print('after', after['data']['name'], after['data']['primaryColor'], after['data']['adBanner'])
assert after['data']['primaryColor'] == '#1565c0'
assert after['data']['adBanner'] == '今日特惠广告位'
assert '换肤' in after['data']['name']

# restore roughly
call('PUT', '/api/agent/site-config', {
    'name': '刘伯温论坛',
    'announcement': '欢迎来到刘伯温论坛（演示站 A）',
    'kefuWechat': 'lbw_kefu',
    'kefuQq': '',
    'primaryColor': '#c62828',
    'fontFamily': 'Microsoft YaHei',
    'logoUrl': '',
    'adBanner': ''
}, auth)
print('M5 checks passed')
