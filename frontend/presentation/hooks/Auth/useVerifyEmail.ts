import { isAxiosError } from "axios";
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  FormEvent,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { VerificationEntity } from "@/domain/entities/verification.entity";
import { AuthService } from "@/presentation/services/AuthService";

export default function useVerifyEmail() {
  const service = useMemo(() => new AuthService(), []);
  const router = useRouter();
  const queryClient = useQueryClient();
  const [metadata, setMetadata] = useState<VerificationEntity | null>(null);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [expired, setExpired] = useState(false);
  const [now, setNow] = useState(0);
  const offset = useRef(0);
  const inFlight = useRef(false);
  const mounted = useRef(true);

  const acceptMetadata = useCallback((data: VerificationEntity) => {
    offset.current = Date.parse(data.serverTime) - Date.now();
    setNow(Date.now() + offset.current);
    setMetadata(data);
    setExpired(false);
  }, []);

  const handleError = useCallback((failure: unknown) => {
    setError(
      isAxiosError(failure)
        ? (failure.response?.data?.message ??
            "Không thể kết nối. Vui lòng thử lại")
        : "Không thể xác thực. Vui lòng thử lại",
    );
    if (isAxiosError(failure)) {
      if (failure.response?.status === 401) setExpired(true);
      const seconds = Number(failure.response?.data?.retryAfter);
      if (seconds > 0)
        setMetadata((previous) =>
          previous
            ? {
                ...previous,
                resendAt: new Date(
                  Date.now() + offset.current + seconds * 1000,
                ).toISOString(),
              }
            : previous,
        );
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await service.verification();
      if (mounted.current) acceptMetadata(data);
    } catch (failure) {
      if (mounted.current) handleError(failure);
    } finally {
      if (mounted.current) setLoading(false);
    }
  }, [service, acceptMetadata, handleError]);

  useEffect(() => {
    mounted.current = true;
    void load();
    const timer = setInterval(() => setNow(Date.now() + offset.current), 1000);
    return () => {
      mounted.current = false;
      clearInterval(timer);
    };
  }, [load]);

  const sessionExpired =
    expired || Boolean(metadata && now >= Date.parse(metadata.expiresAt));
  const resendSeconds = metadata
    ? Math.max(0, Math.ceil((Date.parse(metadata.resendAt) - now) / 1000))
    : 0;
  const otpSeconds = metadata
    ? Math.max(0, Math.ceil((Date.parse(metadata.otpExpiresAt) - now) / 1000))
    : 0;

  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (inFlight.current || sessionExpired || !/^[0-9]{6}$/.test(otp)) return;
    inFlight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await service.verifyOtp(otp);
      await queryClient.cancelQueries();
      queryClient.clear();
      if (mounted.current) {
        router.replace("/");
        router.refresh();
      }
    } catch (failure) {
      if (mounted.current) {
        handleError(failure);
        try {
          const data = await service.verification();
          if (mounted.current) acceptMetadata(data);
        } catch {
          /* Keep the original error. */
        }
      }
    } finally {
      inFlight.current = false;
      if (mounted.current) setBusy(false);
    }
  }

  async function resend() {
    if (inFlight.current || resendSeconds > 0 || sessionExpired) return;
    inFlight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const data = await service.resendOtp();
      if (mounted.current) {
        acceptMetadata(data);
        setOtp("");
        setNotice("Đã gửi OTP mới. Mã cũ không còn hiệu lực.");
      }
    } catch (failure) {
      if (mounted.current) handleError(failure);
    } finally {
      inFlight.current = false;
      if (mounted.current) setBusy(false);
    }
  }

  return {
    metadata,
    otp,
    setOtp,
    error,
    notice,
    loading,
    busy,
    sessionExpired,
    resendSeconds,
    otpSeconds,
    verify,
    resend,
    reload: load,
  };
}
