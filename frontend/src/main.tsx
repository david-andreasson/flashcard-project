import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RootLayout } from './components/RootLayout'
import { HomePage } from './components/HomePage'
import { NotFoundPage } from './components/NotFoundPage'
import { LoginPage } from './components/LoginPage'
import { RegisterPage } from './components/RegisterPage'
import { ProtectedRoute } from './components/ProtectedRoute'
import { CoursesPage } from './components/CoursesPage'
import { CourseDetailPage } from './components/CourseDetailPage'
import { DeckCardsPage } from './components/DeckCardsPage'
import { AiGeneratePage } from './components/AiGeneratePage'
import { StudyPage } from './components/StudyPage'
import { StudyHistoryPage } from './components/StudyHistoryPage'
import './style.css'

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <RootLayout />,
        children: [
          { index: true, element: <HomePage /> },
          { path: 'courses', element: <CoursesPage /> },
          { path: 'courses/:id', element: <CourseDetailPage /> },
          { path: 'courses/:courseId/decks/:deckId', element: <DeckCardsPage /> },
          { path: 'courses/:courseId/decks/:deckId/study', element: <StudyPage /> },
          { path: 'ai/generate', element: <AiGeneratePage /> },
          { path: 'study-history', element: <StudyHistoryPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  </StrictMode>,
)
