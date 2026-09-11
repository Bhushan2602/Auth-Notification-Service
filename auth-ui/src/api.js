import axios from 'axios';
import toast from 'react-hot-toast';

const API = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8090/api',
});

API.interceptors.request.use((config) => {
  const saved = localStorage.getItem('auth_user');
  if (saved) {
    try {
      const { accessToken } = JSON.parse(saved);
      if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
    } catch { /* logged out */ }
  }
  return config;
});

API.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retried) {
      original._retried = true;
      try {
        const saved = JSON.parse(localStorage.getItem('auth_user') || '{}');
        if (saved.refreshToken) {
          const { data } = await axios.post(
            `${API.defaults.baseURL}/auth/refresh`, { refreshToken: saved.refreshToken });
          localStorage.setItem('auth_user', JSON.stringify(data));
          original.headers.Authorization = `Bearer ${data.accessToken}`;
          return API(original);
        }
      } catch { /* fall through to login */ }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (d) => API.post('/auth/register', d),
  login: (d) => API.post('/auth/login', d),
  verify: (token) => API.post('/auth/verify', { token }),
  forgot: (email) => API.post('/auth/forgot', { email }),
  reset: (d) => API.post('/auth/reset', d),
  updateProfile: (d) => API.put('/auth/profile', d),
  changePassword: (d) => API.put('/auth/password', d),
  sessions: () => API.get('/sessions'),
  revokeSession: (id) => API.delete(`/sessions/${id}`),
  revokeAll: () => API.delete('/sessions'),
  inbox: () => API.get('/notifications'),
  unread: () => API.get('/notifications/unread-count'),
  markRead: (id) => API.put(`/notifications/${id}/read`),
};

export default API;
export { toast };
