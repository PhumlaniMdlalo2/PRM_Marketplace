import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Search from './pages/Search';
import ProductDetails from './pages/ProductDetails';
import Login from './pages/Login';
import SignUp from './pages/SignUp';
import Verification from './pages/Verification';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Messages from './pages/Messages';
import Conversation from './pages/Conversation';
import Orders from './pages/Orders';
import Cart from './pages/Cart';
import Bulletin from './pages/Bulletin';
import CreatePost from './pages/CreatePost';
import PostComments from './pages/PostComments';
import Profile from './pages/Profile';
import EditProfile from './pages/EditProfile';
import Settings from './pages/Settings';
import SavedItems from './pages/SavedItems';
import CreateListing from './pages/CreateListing';
import EditListing from './pages/EditListing';
import MyListings from './pages/MyListings';
import OrderDetail from './pages/OrderDetail';
import NotFound from './pages/NotFound';
import ProtectedRoute from './auth/ProtectedRoute';

/**
 * Public routes are the ones the backend serves without a token: browsing the catalogue, reading
 * the bulletin and its comments, and the auth pages themselves.
 *
 * Everything else is wrapped. That list is deliberately not the same as "every route that touches a
 * user": `/bulletin/:id` reads a thread and is public, but the same page has a comment box that
 * needs a session, and that check belongs at the point of use rather than at the router. The rule
 * is that a route is wrapped when *rendering the page at all* is meaningless without an account.
 */
const protectedPage = (element) => <ProtectedRoute>{element}</ProtectedRoute>;

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/search" element={<Search />} />
        <Route path="/product/:id" element={<ProductDetails />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
        <Route path="/verification" element={<Verification />} />
        {/* The reset flow is public because the user has lost the credential that would let them
            authenticate; it is still protected server-side by the single-use, expiring token rather
            than by a session. */}
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        <Route path="/messages" element={protectedPage(<Messages />)} />
        <Route path="/messages/:id" element={protectedPage(<Conversation />)} />
        <Route path="/orders" element={protectedPage(<Orders />)} />
        <Route path="/orders/:id" element={protectedPage(<OrderDetail />)} />
        <Route path="/cart" element={protectedPage(<Cart />)} />
        <Route path="/profile" element={protectedPage(<Profile />)} />
        <Route path="/profile/edit" element={protectedPage(<EditProfile />)} />
        <Route path="/settings" element={protectedPage(<Settings />)} />
        <Route path="/saved" element={protectedPage(<SavedItems />)} />
        <Route path="/listing/create" element={protectedPage(<CreateListing />)} />
        <Route path="/listing/edit/:id" element={protectedPage(<EditListing />)} />
        <Route path="/listing/mine" element={protectedPage(<MyListings />)} />

        <Route path="/bulletin" element={<Bulletin />} />
        <Route path="/bulletin/create" element={protectedPage(<CreatePost />)} />
        <Route path="/bulletin/:id" element={<PostComments />} />

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}

export default App;