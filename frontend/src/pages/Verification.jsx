import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Button from '../components/ui/Button';
import BackButton from '../components/ui/BackButton';
import * as authApi from '../api/auth';
import { useAuth } from '../auth/useAuth';

const RESEND_SECONDS = 30;

const Verification = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { verifyAccount } = useAuth();
  // SignUp hands the address over in router state, so the user is not asked to type an email they
  // just typed. Arriving without it means they followed a link or refreshed, and there is no code
  // to verify against, so they are sent back rather than shown a form that cannot succeed.
  const email = location.state?.email;

  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [countdown, setCountdown] = useState(RESEND_SECONDS);
  const [submitting, setSubmitting] = useState(false);
  const inputRefs = useRef([]);

  useEffect(() => {
    if (!email) navigate('/signup', { replace: true });
  }, [email, navigate]);

  // The countdown never used to run: canResend started false and nothing started a timer, so the
  // button stayed disabled forever. Resending is rate-limited server-side by deleting the previous
  // code, so this timer is the polite half of that — it keeps people from asking for a code that
  // will be cancelled before they read it.
  useEffect(() => {
    if (countdown <= 0) return undefined;
    const timer = setTimeout(() => setCountdown((seconds) => seconds - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const canResend = countdown <= 0;
  
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
    const verificationCode = code.join('');
    if (verificationCode.length < 6) {
      setError('Please enter all 6 digits');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await verifyAccount(email, verificationCode);
      // Verifying returns the updated user but not a token, so there is still no session to
      // store. The user logs in with the password they just chose.
      navigate('/login', { replace: true, state: { email } });
    } catch (caught) {
      setError(caught.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    setCountdown(RESEND_SECONDS);
    setError('');
    try {
      await authApi.resendCode(email);
    } catch (caught) {
      setError(caught.message);
    }
  };
  
  return (
    <div className="min-h-dvh bg-white px-6 py-8">
      <div className="w-full max-w-md mx-auto">
      <BackButton />
      
      <div className="mt-10">
        <h1 className="text-3xl font-bold text-text-primary tracking-tight text-wrap-balance">Almost there</h1>
        <p className="mt-3 text-text-secondary leading-relaxed text-wrap-pretty">
          We sent a 6-digit code to your email. Enter it below to confirm your account.
        </p>
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
            disabled={!canResend}
            className={`font-semibold transition-colors ${canResend ? 'text-primary hover:underline' : 'text-text-muted cursor-not-allowed'}`}
          >
            Resend again
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
