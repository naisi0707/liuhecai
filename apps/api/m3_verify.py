import json
import urllib.request

BASE = 'http://127.0.0.1:8080'

def post(path, body, headers=None):
    h = {'Content-Type': 'application/json'}
    if headers:
        h.update(headers)
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode('utf-8'), headers=h, method='POST')
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode('utf-8')

def get(path, headers=None):
    h = {}
    if headers:
        h.update(headers)
    req = urllib.request.Request(BASE + path, headers=h, method='GET')
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode('utf-8')

admin = json.loads(post('/api/admin/auth/login', {'username': 'admin', 'password': 'admin123'}))
print('admin-login', admin)
admin_token = admin['data']['token']
print('admin-me', get('/api/admin/me', {'Authorization': f'Bearer {admin_token}'}))

agent = json.loads(post('/api/agent/auth/login', {'username': 'agent_a', 'password': 'agent123'}))
print('agent-login', agent)
agent_token = agent['data']['token']
print('agent-me', get('/api/agent/me', {'Authorization': f'Bearer {agent_token}', 'X-Forwarded-Host': 'lbw.local'}))
print('agent-on-admin', get('/api/admin/me', {'Authorization': f'Bearer {agent_token}'}))

user = json.loads(post('/api/user/auth/register', {'username': 'user_m3', 'password': 'user1234'}, {'X-Forwarded-Host': 'lbw.local'}))
print('user-register', user)
user_token = user['data']['token']
print('user-me', get('/api/user/me', {'Authorization': f'Bearer {user_token}', 'X-Forwarded-Host': 'lbw.local'}))
print('user-on-agent', get('/api/agent/me', {'Authorization': f'Bearer {user_token}', 'X-Forwarded-Host': 'lbw.local'}))
