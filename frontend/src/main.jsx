import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from "react-router-dom";
import './styles/index.css'
import App from './app/App.jsx'
import AuthProvider from './auth/AuthProvider.jsx'
import Toaster from './components/Toaster.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
      <BrowserRouter>
          <AuthProvider>
              <Toaster>
                  <App />
              </Toaster>
          </AuthProvider>
      </BrowserRouter>
  </StrictMode>,
)
