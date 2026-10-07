import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import Input from '../components/ui/Input';
import * as authApi from '../api/auth';
import { useAuth } from '../auth/useAuth';

const RESEND_SECONDS = 30;

const Verification = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { verifyAccount } = useAuth();
  const [email, setEmail] = useState(location.state?.email ?? '');

  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const inputRefs = useRef([]);

  // A successful resend starts a short cooldown, but users arriving here from login or after a
  // refresh can request the first code immediately.
  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = setTimeout(() => setCountdown((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const canResend = countdown <= 0;

  const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());

  const handleEmailChange = (event) => {
    setEmail(event.target.value);
    setError('');
    setNotice('');
  };

  const handleChange = (index, value) => {
    if (value.length > 1) return;

    const newCode = [...code];
    newCode[index] = value.replace(/\D/g, '');
    setCode(newCode);
    setError('');

    if (value && index < 5) {
      inputRefs.current[index + 1].focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      inputRefs.current[index - 1].focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted) {
      const newCode = pasted.split('');
      while (newCode.length < 6) newCode.push('');
      setCode(newCode);
      inputRefs.current[Math.min(pasted.length - 1, 5)].focus();
    }
  };

  const handleVerify = async () => {
    if (!validEmail) {
      setError('Enter a valid email address');
      return;
    }

    const verificationCode = code.join('');
    if (verificationCode.length < 6) {
      setError('Please enter all 6 digits');
      return;
    }

    setSubmitting(true);
    setError('');
    setNotice('');
    try {
      await verifyAccount(email.trim(), verificationCode);
      // Verifying returns the updated user but not a token, so there is still no session to
      // store. The user logs in with the password they just chose.
      navigate('/login', { replace: true, state: { email: email.trim() } });
    } catch (caught) {
      setError(caught.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    if (!validEmail) {
      setError('Enter a valid email address');
      return;
    }

    setResending(true);
    setError('');
    setNotice('');
    try {
      await authApi.resendCode(email.trim());
      setCountdown(RESEND_SECONDS);
      setNotice('A new verification code was sent. Check your inbox and spam folder.');
    } catch (caught) {
      setError(caught.message);
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="min-h-dvh bg-white px-6 py-8">
      <div className="w-full max-w-md mx-auto">
      <BackButton />
      
      <div className="mt-10">
        <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">Almost there</h1>
        <p className="mt-3 text-text-secondary leading-relaxed text-wrap-pretty">
          Enter the 6-digit code sent to your email. If you did not receive one, request a new code.
        </p>
      </div>

      <div className="mt-8">
        <Input
          label="Email"
          type="email"
          name="email"
          placeholder="you@university.ac.za"
          value={email}
          onChange={handleEmailChange}
          autoComplete="email"
        />
      </div>

      <div 
        className="mt-10 flex justify-center gap-2.5" 
        onPaste={handlePaste}
        aria-label="6-digit verification code"
      >
        {code.map((digit, index) => (
          <input
            key={index}
            ref={(el) => (inputRefs.current[index] = el)}
            type="text"
            inputMode="numeric"
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            maxLength={1}
            value={digit}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            aria-label={`Digit ${index + 1}`}
            className={`w-12 h-14 text-center text-xl font-semibold bg-white border rounded-xl focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all duration-200 ${error ? 'border-error' : 'border-border'}`}
          />
        ))}
      </div>
      
      {error && (
        <p className="mt-3 text-center text-sm text-error">{error}</p>
      )}
      {notice && (
        <p className="mt-3 text-center text-sm text-success" role="status">{notice}</p>
      )}

      <div className="mt-8">
        <Button onClick={handleVerify} size="lg" disabled={submitting}>
          {submitting ? 'Verifying…' : 'Verify'}
        </Button>
      </div>

      <div className="mt-6 text-center">
        <p className="text-sm text-text-secondary text-wrap-pretty">
          Didn't receive any code?{' '}
          <button
            type="button"
            onClick={handleResend}
            disabled={!canResend || resending}
            className={`font-semibold transition-colors ${canResend && !resending ? 'text-primary hover:underline' : 'text-text-muted cursor-not-allowed'}`}
          >
            {resending ? 'Sending…' : 'Resend code'}
          </button>
        </p>
        {!canResend && (
          <p className="mt-2 text-xs text-text-muted">
            You can request a new code in 00:{String(countdown).padStart(2, '0')}
          </p>
        )}
      </div>
      </div>
    </div>
  );
};

export default Verification;
