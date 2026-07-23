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

admin = call('POST', '/api/admin/auth/login', {'username': 'admin', 'password': 'admin123'})
token = admin['data']['token']
auth = {'Authorization': f'Bearer {token}'}
print('login', admin['code'])

created = call('POST', '/api/admin/tenants', {
    'name': '神算子论坛',
    'primaryHost': 'ssz.local',
    'agentUsername': 'agent_ssz',
    'announcement': '欢迎来到神算子（M4新建）'
}, auth)
print('create', created)
assert created['code'] == 0, created
tenant_id = created['data']['tenant']['id']
raw_pwd = created['data']['agent']['rawPassword']
print('agent password once:', raw_pwd)

current = call('GET', '/api/tenant/current', headers={'X-Forwarded-Host': 'ssz.local'})
print('new host tenant', current)
assert current['data']['name'] == '神算子论坛'

disabled = call('PUT', f'/api/admin/tenants/{tenant_id}/status', {'status': 0}, auth)
print('disable', disabled['code'])
blocked = call('GET', '/api/tenant/current', headers={'X-Forwarded-Host': 'ssz.local'})
print('disabled host', blocked)
assert blocked['code'] == 30003

enabled = call('PUT', f'/api/admin/tenants/{tenant_id}/status', {'status': 1}, auth)
print('enable', enabled['code'])
ok_again = call('GET', '/api/tenant/current', headers={'X-Forwarded-Host': 'ssz.local'})
print('enabled host', ok_again['data']['name'])

print('M4 API checks passed')
