import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import Search from './pages/Search';
import ProductDetails from './pages/ProductDetails';
import Login from './pages/Login';
import SignUp from './pages/SignUp';
import Verification from './pages/Verification';
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
import NotFound from './pages/NotFound';

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
        <Route path="/messages" element={<Messages />} />
        <Route path="/messages/:id" element={<Conversation />} />
        <Route path="/orders" element={<Orders />} />
        <Route path="/cart" element={<Cart />} />
        <Route path="/bulletin" element={<Bulletin />} />
        <Route path="/bulletin/create" element={<CreatePost />} />
        <Route path="/bulletin/:id" element={<PostComments />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/profile/edit" element={<EditProfile />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="/saved" element={<SavedItems />} />
        <Route path="/listing/create" element={<CreateListing />} />
        <Route path="/listing/edit/:id" element={<EditListing />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}

export default App;
