import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useNavigate, useSearchParams, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import toast from 'react-hot-toast';
import { AuthProvider, useAuth } from './auth';
import { authApi } from './api';

const shell = 'min-h-screen bg-gray-50 flex items-center justify-center p-4';
const card = 'w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-100 p-8';
const input = 'w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-900 focus:outline-none focus:border-orange-500';
const btn = 'w-full bg-orange-500 text-white font-black py-3.5 rounded-xl hover:bg-orange-600 transition disabled:bg-gray-300';

function Login() {
  const nav = useNavigate();
  const { login } = useAuth();
  const [f, setF] = useState({ email: '', password: '' });
  const submit = async (e) => {
    e.preventDefault();
    try {
      const { data } = await authApi.login({ ...f, deviceLabel: navigator.userAgent.slice(0, 60) });
      login(data);
      toast.success('Welcome back!');
      nav('/inbox');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed');
    }
  };
  return (
    <div className={shell}>
      <form onSubmit={submit} className={card + ' space-y-4'}>
        <h1 className="text-2xl font-black">Sign in</h1>
        <input className={input} placeholder="Email" type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
        <input className={input} placeholder="Password" type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
        <button className={btn}>Login</button>
        <div className="flex justify-between text-sm font-bold">
          <Link to="/register" className="text-orange-600">Register</Link>
          <Link to="/forgot" className="text-orange-600">Forgot?</Link>
        </div>
        <a href="http://localhost:8090/oauth2/authorization/google" className="block text-center bg-white border border-gray-200 rounded-xl py-3 text-sm font-black hover:bg-gray-50">Continue with Google</a>
      </form>
    </div>
  );
}

function Register() {
  const nav = useNavigate();
  const [f, setF] = useState({ fullName: '', email: '', password: '' });
  const submit = async (e) => {
    e.preventDefault();
    try {
      await authApi.register(f);
      toast.success('Registered! Check your email (or MailHog :8026 in docker).');
      nav('/login');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Register failed');
    }
  };
  return (
    <div className={shell}>
      <form onSubmit={submit} className={card + ' space-y-4'}>
        <h1 className="text-2xl font-black">Create account</h1>
        <input className={input} placeholder="Full name" required value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} />
        <input className={input} placeholder="Email" type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
        <input className={input} placeholder="Password (min 6)" type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
        <button className={btn}>Register</button>
        <Link to="/login" className="block text-center text-sm font-bold text-orange-600">Back to login</Link>
      </form>
    </div>
  );
}

function Verify() {
  const [p] = useSearchParams();
  const [msg, setMsg] = useState('Verifying...');
  useEffect(() => {
    authApi.verify(p.get('token') || '').then(
      () => setMsg('Email verified! You can now login.'),
      (e) => setMsg(e.response?.data?.message || 'Verification failed.')
    );
  }, []);
  return <div className={shell}><div className={card}><p className="font-bold">{msg}</p><Link to="/login" className="text-orange-600 font-bold text-sm">Go to login</Link></div></div>;
}

function Forgot() {
  const [email, setEmail] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    await authApi.forgot(email);
    toast.success('If the account exists, a reset link was sent.');
  };
  return (
    <div className={shell}>
      <form onSubmit={submit} className={card + ' space-y-4'}>
        <h1 className="text-2xl font-black">Reset password</h1>
        <input className={input} placeholder="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <button className={btn}>Send reset link</button>
      </form>
    </div>
  );
}

function Reset() {
  const [p] = useSearchParams();
  const nav = useNavigate();
  const [pw, setPw] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    try {
      await authApi.reset({ token: p.get('token') || '', newPassword: pw });
      toast.success('Password reset! Login again.');
      nav('/login');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Reset failed');
    }
  };
  return (
    <div className={shell}>
      <form onSubmit={submit} className={card + ' space-y-4'}>
        <h1 className="text-2xl font-black">New password</h1>
        <input className={input} placeholder="New password (min 6)" type="password" required value={pw} onChange={(e) => setPw(e.target.value)} />
        <button className={btn}>Reset</button>
      </form>
    </div>
  );
}

function Guard({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

function Inbox() {
  const { user, logout } = useAuth();
  const [items, setItems] = useState([]);
  const [sessions, setSessions] = useState([]);
  useEffect(() => {
    authApi.inbox().then((r) => setItems(r.data)).catch(() => {});
    authApi.sessions().then((r) => setSessions(r.data)).catch(() => {});
  }, []);
  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-2xl mx-auto space-y-4">
        <div className="bg-white rounded-2xl p-5 flex items-center justify-between border">
          <div><p className="font-black">{user?.fullName}</p><p className="text-xs text-gray-500">{user?.email}</p></div>
          <button onClick={logout} className="text-xs font-black text-red-600">Logout</button>
        </div>
        <div className="bg-white rounded-2xl p-5 border">
          <h2 className="font-black mb-3">Notifications</h2>
          {items.length === 0 && <p className="text-sm text-gray-400">No notifications yet.</p>}
          {items.map((n) => (
            <div key={n.id} className="border-b py-2 flex justify-between items-center">
              <div><p className="text-sm font-bold">{n.type}</p><p className="text-xs text-gray-500">{n.message}</p></div>
              {!n.read && <button onClick={() => authApi.markRead(n.id).then(() => setItems(items.map((x) => x.id === n.id ? { ...x, read: true } : x)))} className="text-xs font-black text-orange-600">Mark read</button>}
            </div>
          ))}
        </div>
        <div className="bg-white rounded-2xl p-5 border">
          <h2 className="font-black mb-3">Sessions</h2>
          {sessions.map((s) => (
            <div key={s.id} className="border-b py-2 flex justify-between items-center text-sm">
              <span>{s.device} <span className="text-gray-400 text-xs">exp {s.expiresAt?.slice(0, 10)}</span></span>
              <button onClick={() => authApi.revokeSession(s.id).then(() => setSessions(sessions.filter((x) => x.id !== s.id)))} className="text-xs font-black text-red-600">Revoke</button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Toaster position="top-center" />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify" element={<Verify />} />
          <Route path="/forgot" element={<Forgot />} />
          <Route path="/reset" element={<Reset />} />
          <Route path="/inbox" element={<Guard><Inbox /></Guard>} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
