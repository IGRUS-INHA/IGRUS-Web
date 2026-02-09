import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useUIStore } from '@/stores';
import { User, Lock, Mail, ArrowRight, Phone, GraduationCap, Building2, FileText, Eye, EyeOff, ChevronDown } from 'lucide-react';
import { majorOptions } from '@/constants/majorOptions';
import { domainOptions } from '@/constants/domainOptions';
import { WISH_TITLE, wishOptions } from '@/constants/wishOptions';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import type { ReactNode } from 'react';

interface AuthFormData {
  studentId: string;
  name: string;
  email: string;
  password: string;
  passwordConfirm: string;
  phoneNumber?: string;
  department?: string;
  motivation?: string;
  wishes?: string[];
  gender?: 'MALE' | 'FEMALE';
  grade?: number;
  privacyConsent?: boolean;
}

interface AuthFormProps {
  mode?: 'login' | 'signup';
  icon?: ReactNode;
  title: string;
  subtitle: string;
  onSubmit?: (data: AuthFormData) => void;
  loading?: boolean;
  errors?: {
    studentId?: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    password?: string;
    passwordConfirm?: string;
    department?: string;
    grade?: string;
    gender?: string;
    motivation?: string;
    privacyConsent?: string;
  };
}

export default function AuthForm({
  mode = 'login',
  icon,
  title,
  subtitle,
  onSubmit,
  loading = false,
  errors = {},
}: AuthFormProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const [form, setForm] = useState<AuthFormData>({
    studentId: '',
    name: '',
    email: '',
    password: '',
    passwordConfirm: '',
    phoneNumber: '',
    department: '',
    motivation: '',
    gender: undefined,
    grade: undefined,
    privacyConsent: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [localErrors, setLocalErrors] = useState<{ passwordConfirm?: string }>({});
  const [emailLocal, setEmailLocal] = useState('');
  const [emailDomain, setEmailDomain] = useState('inha.edu');
  const [customDomain, setCustomDomain] = useState('');
  const [selectedWishes, setSelectedWishes] = useState<string[]>([]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;

    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setForm({ ...form, [name]: checked });
    } else if (name === 'grade') {
      setForm({ ...form, [name]: value ? parseInt(value) : undefined });
    } else if (name === 'phoneNumber') {
      const digitsOnly = value.replace(/\D/g, '').slice(0, 11);
      setForm({ ...form, [name]: digitsOnly });
    } else if (name === 'emailLocal') {
      setEmailLocal(value);
    } else if (name === 'emailDomain') {
      setEmailDomain(value);
      if (value !== 'custom') {
        setCustomDomain('');
      }
    } else if (name === 'customDomain') {
      setCustomDomain(value);
    } else {
      setForm({ ...form, [name]: value });
    }
  };

  const handleWishToggle = (wish: string) => {
    setSelectedWishes((prev) =>
      prev.includes(wish) ? prev.filter((w) => w !== wish) : [...prev, wish],
    );
  };

  const handlePasswordConfirmBlur = () => {
    if (form.passwordConfirm && form.passwordConfirm !== form.password) {
      setLocalErrors({ passwordConfirm: '비밀번호가 일치하지 않습니다.' });
    } else {
      setLocalErrors({});
    }
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const domain = emailDomain === 'custom' ? customDomain : emailDomain;
    const fullEmail = `${emailLocal}@${domain}`.trim();
    onSubmit?.({ ...form, email: fullEmail, wishes: selectedWishes });
  };

  const isLogin = mode === 'login';
  const passwordConfirmError = errors.passwordConfirm || localErrors.passwordConfirm;

  return (
    <Card className={`${isLogin ? 'p-s4' : 'p-s1'} lg:p-s7 rounded-r4 border ${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}>
      <CardContent className="p-0">
        <div className={`mb-s6 ${icon ? 'text-center' : 'text-left pt-s4'}`}>
          {icon && (
            <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
              {icon}
            </div>
          )}
          <h2 className="text-h2 mb-s2">{title}</h2>
          <p className="text-muted-foreground text-b2">{subtitle}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-s4">
          <div>
            <div className="relative">
              <User size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="text"
                name="studentId"
                placeholder="학번 (8자리)"
                value={form.studentId}
                onChange={handleChange}
                required
                className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                  errors.studentId
                    ? 'border-red-500 focus:border-red-500'
                    : 'focus:border-primary border-border'
                } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
              />
            </div>
            {errors.studentId && (
              <p className="mt-s1 text-sm text-red-500">{errors.studentId}</p>
            )}
          </div>

          {!isLogin && (
            <>
              <div>
                <div className="relative">
                  <User size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    type="text"
                    name="name"
                    placeholder="이름"
                    value={form.name}
                    onChange={handleChange}
                    required
                    className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                      errors.name
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                  />
                </div>
                {errors.name && (
                  <p className="mt-s1 text-sm text-red-500">{errors.name}</p>
                )}
              </div>

              <div>
                <div className="flex items-center gap-s2">
                  <div className="relative flex-1">
                    <Mail size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      type="text"
                      name="emailLocal"
                      placeholder="이메일 아이디"
                      value={emailLocal}
                      onChange={handleChange}
                      required
                      className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                        errors.email
                          ? 'border-red-500 focus:border-red-500'
                          : 'focus:border-primary border-border'
                      } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                    />
                  </div>
                  <span className="text-muted-foreground font-bold shrink-0">@</span>
                  <div className="relative flex-1">
                    <select
                      name="emailDomain"
                      value={emailDomain}
                      onChange={handleChange}
                      className={`w-full rounded-r4 px-s4 py-s6 border transition-all appearance-none cursor-pointer ${
                        errors.email
                          ? 'border-red-500 focus:border-red-500'
                          : 'focus:border-primary border-border'
                      } ${isDark ? 'bg-white/5 text-foreground' : 'bg-muted text-foreground'}`}
                    >
                      {domainOptions.map((domain) => (
                        <option key={domain.value} value={domain.value}>
                          {domain.label}
                        </option>
                      ))}
                    </select>
                    <ChevronDown size={16} className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                  </div>
                </div>
                {emailDomain === 'custom' && (
                  <div className="mt-s2">
                    <Input
                      type="text"
                      name="customDomain"
                      placeholder="도메인 입력 (예: example.com)"
                      value={customDomain}
                      onChange={handleChange}
                      required
                      className={`w-full rounded-r4 px-s4 py-s6 border transition-all ${
                        errors.email
                          ? 'border-red-500 focus:border-red-500'
                          : 'focus:border-primary border-border'
                      } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                    />
                  </div>
                )}
                {errors.email && (
                  <p className="mt-s1 text-sm text-red-500">{errors.email}</p>
                )}
              </div>

              <div>
                <div className="relative">
                  <Phone size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    type="tel"
                    name="phoneNumber"
                    placeholder="전화번호 (숫자만 입력)"
                    maxLength={11}
                    value={form.phoneNumber}
                    onChange={handleChange}
                    required
                    className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                      errors.phoneNumber
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                  />
                </div>
                {errors.phoneNumber && (
                  <p className="mt-s1 text-sm text-red-500">{errors.phoneNumber}</p>
                )}
              </div>

              <div>
                <div className="relative">
                  <Building2 size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <select
                    name="department"
                    value={form.department}
                    onChange={handleChange}
                    required
                    className={`w-full rounded-r4 pl-12 pr-10 py-s6 border transition-all appearance-none cursor-pointer ${
                      errors.department
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5 text-foreground' : 'bg-muted text-foreground'}`}
                  >
                    <option value="">학과를 선택하세요</option>
                    {majorOptions.map((college) => (
                      <optgroup key={college.title} label={college.title}>
                        {college.items.map((dept) => (
                          <option key={dept.key} value={dept.value}>
                            {dept.value}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                  <ChevronDown size={16} className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                </div>
                {errors.department && (
                  <p className="mt-s1 text-sm text-red-500">{errors.department}</p>
                )}
              </div>

              <div>
                <div className="relative">
                  <GraduationCap size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    type="number"
                    name="grade"
                    placeholder="학년 (1~4)"
                    value={form.grade || ''}
                    onChange={handleChange}
                    required
                    min="1"
                    max="4"
                    className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                      errors.grade
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                  />
                </div>
                {errors.grade && (
                  <p className="mt-s1 text-sm text-red-500">{errors.grade}</p>
                )}
              </div>

              <div>
                <div className="relative">
                  <User size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <select
                    name="gender"
                    value={form.gender || ''}
                    onChange={handleChange}
                    required
                    className={`w-full rounded-r4 pl-12 pr-10 py-s6 border transition-all appearance-none cursor-pointer ${
                      errors.gender
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5 text-foreground' : 'bg-muted text-foreground'}`}
                  >
                    <option value="">성별 선택</option>
                    <option value="MALE">남성</option>
                    <option value="FEMALE">여성</option>
                  </select>
                  <ChevronDown size={16} className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
                </div>
                {errors.gender && (
                  <p className="mt-s1 text-sm text-red-500">{errors.gender}</p>
                )}
              </div>

              <div>
                <p className="text-sm font-medium text-foreground mb-s2">{WISH_TITLE}</p>
                <div className="flex flex-wrap gap-s2">
                  {wishOptions.map((wish) => (
                    <button
                      key={wish}
                      type="button"
                      onClick={() => handleWishToggle(wish)}
                      className={`px-s4 py-s2 rounded-full border text-sm transition-all cursor-pointer ${
                        selectedWishes.includes(wish)
                          ? 'bg-primary text-primary-foreground border-primary'
                          : `border-border ${isDark ? 'bg-white/5 text-foreground' : 'bg-muted text-foreground'} hover:border-primary`
                      }`}
                    >
                      {wish}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <div className="relative">
                  <FileText size={18} className="absolute left-s4 top-s3 text-muted-foreground" />
                  <textarea
                    name="motivation"
                    placeholder="동아리 가입 동기를 작성해주세요"
                    value={form.motivation}
                    onChange={handleChange}
                    required
                    rows={4}
                    className={`w-full rounded-r4 pl-12 pr-s4 py-s3 border transition-all resize-none ${
                      errors.motivation
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5 text-foreground placeholder:text-muted-foreground' : 'bg-muted text-foreground placeholder:text-muted-foreground'}`}
                  />
                </div>
                {errors.motivation && (
                  <p className="mt-s1 text-sm text-red-500">{errors.motivation}</p>
                )}
              </div>
            </>
          )}

          <div>
            <div className="relative group">
              <Lock size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type={showPassword ? 'text' : 'password'}
                name="password"
                placeholder="비밀번호 (영문, 숫자 포함 8자 이상)"
                value={form.password}
                onChange={handleChange}
                required
                className={`w-full rounded-r4 pl-12 pr-12 py-s6 border transition-all ${
                  errors.password
                    ? 'border-red-500 focus:border-red-500'
                    : 'focus:border-primary border-border'
                } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
              />
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-all opacity-0 group-focus-within:opacity-100"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.password && (
              <p className="mt-s1 text-sm text-red-500">{errors.password}</p>
            )}
          </div>

          {!isLogin && (
            <>
              <div>
                <div className="relative group">
                  <Lock size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    type={showPasswordConfirm ? 'text' : 'password'}
                    name="passwordConfirm"
                    placeholder="비밀번호 확인"
                    value={form.passwordConfirm}
                    onChange={handleChange}
                    onBlur={handlePasswordConfirmBlur}
                    required
                    className={`w-full rounded-r4 pl-12 pr-12 py-s6 border transition-all ${
                      passwordConfirmError
                        ? 'border-red-500 focus:border-red-500'
                        : 'focus:border-primary border-border'
                    } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
                  />
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                    className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-all opacity-0 group-focus-within:opacity-100"
                  >
                    {showPasswordConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {passwordConfirmError && (
                  <p className="mt-s1 text-sm text-red-500">{passwordConfirmError}</p>
                )}
              </div>

              <div>
                <div className="flex items-start gap-s2">
                  <input
                    type="checkbox"
                    name="privacyConsent"
                    id="privacyConsent"
                    checked={form.privacyConsent}
                    onChange={handleChange}
                    required
                    className="mt-s1 rounded-r1"
                  />
                  <label htmlFor="privacyConsent" className="text-sm text-muted-foreground cursor-pointer">
                    개인정보 처리방침에 동의합니다 (필수)
                  </label>
                </div>
                {errors.privacyConsent && (
                  <p className="mt-s1 text-sm text-red-500">{errors.privacyConsent}</p>
                )}
              </div>
            </>
          )}

          <Button
            type="submit"
            disabled={loading}
            className="w-full py-s6 rounded-r4 font-bold flex items-center justify-center gap-s2 shadow-lg shadow-primary/20"
          >
            {loading ? (isLogin ? '로그인 중...' : '가입 중...') : (isLogin ? '로그인' : '회원가입')}
            <ArrowRight size={18} />
          </Button>
        </form>

        <div className="mt-s5 text-center space-y-s3">
          {isLogin ? (
            <>
              <Link
                to="/forgot-password"
                className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                비밀번호를 잊으셨나요?
              </Link>
              <Link
                to="/signup"
                className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                계정이 없으신가요? 회원가입
              </Link>
            </>
          ) : (
            <Link
              to="/login"
              className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest"
            >
              이미 계정이 있으신가요? 로그인
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
