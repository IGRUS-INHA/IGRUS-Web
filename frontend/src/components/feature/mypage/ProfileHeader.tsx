import { useState } from 'react';
import { useUIStore } from '@/stores';
import { User, Mail, Calendar, Edit3, Shield, Lock, LogOut, UserX, Building2, Phone, Pencil, Check, X, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ROLE_LABELS } from '@/constants';
import { formatPhoneNumber } from '@/utils';
import { cn } from '@/lib/utils';
import type { User as UserType } from '@/types/entities';
import type { MyProfileResponse } from '@/api/model/models/myProfileResponse';
import type { UpdateProfileRequest } from '@/api/model/models/updateProfileRequest';

interface ProfileHeaderProps {
  user: UserType;
  profile?: MyProfileResponse | undefined;
  onChangePassword?: () => void;
  onLogout?: () => void;
  onWithdraw?: () => void;
  onUpdateProfile?: (data: UpdateProfileRequest) => Promise<void>;
  isUpdating?: boolean;
}

type EditingField = 'email' | 'phone' | undefined;

export default function ProfileHeader({ user, profile, onChangePassword, onLogout, onWithdraw, onUpdateProfile, isUpdating }: ProfileHeaderProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [editingField, setEditingField] = useState<EditingField>();
  const [editValue, setEditValue] = useState('');

  const startEditing = (field: 'email' | 'phone') => {
    if (field === 'email') {
      setEditValue(profile?.email ?? user.email ?? '');
    } else {
      setEditValue(profile?.phoneNumber ?? '');
    }
    setEditingField(field);
    setEditError('');
  };

  const cancelEditing = () => {
    setEditingField(undefined);
    setEditValue('');
    setEditError('');
  };

  const [editError, setEditError] = useState('');

  const validate = (): string => {
    if (!editingField) return '';
    const value = editValue.trim();
    if (!value) return editingField === 'email' ? '이메일을 입력해주세요.' : '전화번호를 입력해주세요.';
    if (editingField === 'email') {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '올바른 이메일 형식이 아닙니다.';
    } else {
      if (!/^01[0-9]-?\d{3,4}-?\d{4}$/.test(value)) return '올바른 전화번호 형식이 아닙니다. (예: 01012345678)';
    }
    return '';
  };

  const handleSave = async () => {
    if (!onUpdateProfile || !editingField) return;

    const error = validate();
    if (error) {
      setEditError(error);
      return;
    }

    const data: UpdateProfileRequest = editingField === 'email'
      ? { email: editValue.trim() }
      : { phoneNumber: editValue.trim().replace(/-/g, '') };

    await onUpdateProfile(data);
    setEditingField(undefined);
    setEditValue('');
    setEditError('');
  };

  return (
    <Card
      className={`p-s6 lg:p-s7 rounded-[2.5rem] border flex flex-col md:flex-row items-center gap-s6 ${
        isDark
          ? 'bg-gradient-to-br from-card to-background border-border'
          : 'bg-card border-border shadow-xl shadow-black/5'
      }`}
    >
      <div className="relative">
        <div className="w-32 h-32 rounded-[2.5rem] bg-primary/20 border border-primary/50 flex items-center justify-center">
          <User size={64} className="text-primary" />
        </div>
        {__FEATURE_PROFILE_EDIT__ && (
          <button
            type="button"
            className="absolute -bottom-2 -right-2 w-10 h-10 bg-foreground text-background rounded-r4 flex items-center justify-center hover:bg-primary transition border-2 border-background cursor-pointer"
          >
            <Edit3 size={16} />
          </button>
        )}
      </div>

      <div className="flex-1 text-center md:text-left">
        <div className="flex flex-col md:flex-row items-center gap-s3 mb-2">
          <h2 className="typo-h1">{profile?.name ?? user.name}</h2>
          <span className="px-3 py-1 bg-primary/20 text-primary rounded-full typo-c2 font-bold uppercase tracking-widest border border-primary/30">
            {ROLE_LABELS[user.role]}
          </span>
        </div>
        {/* 학번 / 학과 / 가입일 */}
        <div className="flex flex-wrap justify-center md:justify-start gap-s5 text-muted-foreground typo-b2">
          <div className="flex items-center gap-2">
            <Shield size={16} className="text-primary" />
            {profile?.studentId ?? user.studentId}
          </div>
          {profile?.department && (
            <div className="flex items-center gap-2">
              <Building2 size={16} className="text-primary" />
              {profile.department}
            </div>
          )}
          <div className="flex items-center gap-2">
            <Calendar size={16} className="text-primary" />
            가입일 {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('ko-KR') : user.joinedDate}
          </div>
        </div>

        {/* 이메일 / 전화번호 (인라인 수정) */}
        <div className="mt-s3 space-y-s2 text-muted-foreground typo-b2">
          {/* 이메일 */}
          <div className="flex items-start justify-center md:justify-start gap-2">
            <Mail size={16} className="text-primary shrink-0 mt-1.5" />
            {editingField === 'email' ? (
              <div>
                <div className="flex items-center gap-1">
                  <input
                    type="email"
                    value={editValue}
                    onChange={(e) => { setEditValue(e.target.value); setEditError(''); }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') void handleSave();
                      if (e.key === 'Escape') cancelEditing();
                    }}
                    className={cn(
                      'px-2 py-1 typo-b2 bg-background border rounded-md focus:outline-none focus:ring-1 focus:ring-primary w-56',
                      editError ? 'border-destructive' : 'border-border'
                    )}
                    autoFocus
                    disabled={isUpdating}
                  />
                  <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-primary/10 text-primary transition-colors disabled:opacity-50"
                  >
                    {isUpdating ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                  </button>
                  <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-destructive/10 text-destructive transition-colors disabled:opacity-50"
                  >
                    <X size={14} />
                  </button>
                </div>
                {editError && <p className="text-destructive typo-c2 mt-1">{editError}</p>}
              </div>
            ) : (
              <>
                <span>{profile?.email ?? user.email}</span>
                {onUpdateProfile && (
                  <button
                    type="button"
                    onClick={() => startEditing('email')}
                    className="cursor-pointer ml-1 p-1 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
                  >
                    <Pencil size={14} />
                  </button>
                )}
              </>
            )}
          </div>

          {/* 전화번호 */}
          <div className="flex items-start justify-center md:justify-start gap-2">
            <Phone size={16} className="text-primary shrink-0 mt-1.5" />
            {editingField === 'phone' ? (
              <div>
                <div className="flex items-center gap-1">
                  <input
                    type="tel"
                    value={editValue}
                    onChange={(e) => { setEditValue(e.target.value); setEditError(''); }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') void handleSave();
                      if (e.key === 'Escape') cancelEditing();
                    }}
                    placeholder="01012345678"
                    className={cn(
                      'px-2 py-1 typo-b2 bg-background border rounded-md focus:outline-none focus:ring-1 focus:ring-primary w-44',
                      editError ? 'border-destructive' : 'border-border'
                    )}
                    autoFocus
                    disabled={isUpdating}
                  />
                  <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-primary/10 text-primary transition-colors disabled:opacity-50"
                  >
                    {isUpdating ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                  </button>
                  <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-destructive/10 text-destructive transition-colors disabled:opacity-50"
                  >
                    <X size={14} />
                  </button>
                </div>
                {editError && <p className="text-destructive typo-c2 mt-1">{editError}</p>}
              </div>
            ) : (
              <>
                <span>{profile?.phoneNumber ? formatPhoneNumber(profile.phoneNumber) : '-'}</span>
                {onUpdateProfile && (
                  <button
                    type="button"
                    onClick={() => startEditing('phone')}
                    className="cursor-pointer ml-1 p-1 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
                  >
                    <Pencil size={14} />
                  </button>
                )}
              </>
            )}
          </div>
        </div>

        {(onChangePassword || onLogout || onWithdraw) && (
          <div className="flex flex-wrap justify-center md:justify-start gap-s3 mt-s4">
            {onChangePassword && (
              <Button
                type="button"
                onClick={onChangePassword}
                variant="outline"
                className={`flex items-center gap-s2 rounded-r3 text-xs font-bold ${
                  isDark ? 'border-border hover:bg-white/5' : 'border-border hover:bg-muted'
                }`}
              >
                <Lock size={14} /> 비밀번호 변경
              </Button>
            )}
            {onLogout && (
              <Button
                type="button"
                onClick={onLogout}
                variant="outline"
                className="flex items-center gap-s2 rounded-r3 text-xs font-bold border-destructive/30 text-destructive hover:bg-destructive/10"
              >
                <LogOut size={14} /> 로그아웃
              </Button>
            )}
            {onWithdraw && (
              <Button
                type="button"
                onClick={onWithdraw}
                variant="outline"
                className="flex items-center gap-s2 rounded-r3 text-xs font-bold border-destructive/30 text-destructive hover:bg-destructive/10"
              >
                <UserX size={14} /> 회원 탈퇴
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  );
}
