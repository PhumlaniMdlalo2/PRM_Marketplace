import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './auth/AuthContext'

// AuthProvider sits above the router rather than inside App so that the 401 interceptor it
// registers can navigate, and so every route — including the login and signup pages — can read
// session state instead of each one parsing storage itself.
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)