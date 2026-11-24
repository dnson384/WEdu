"use client";

import Link from "next/link";
import useVerifyEmail from "@/presentation/hooks/Auth/useVerifyEmail";

export default function VerifyEmailPage() {
  const state = useVerifyEmail();
  return (
    <main className="min-h-screen bg-gray-50 px-4 py-30">
      <section className="mx-auto max-w-md rounded-2xl border border-gray-200 bg-white p-6 shadow-sm sm:p-10">
        <h1 className="text-2xl font-bold text-gray-900">Xác thực email</h1>
        {state.loading ? (
          <p className="mt-4" role="status">
            Đang tải phiên xác thực...
          </p>
        ) : (
          <>
            {state.metadata && (
              <p className="mt-3 text-sm text-gray-600">
                Nhập mã gồm 6 chữ số đã gửi đến {state.metadata.email}.
              </p>
            )}

            {state.error && (
              <p role="alert" className="mt-4 text-sm text-red-600">
                {state.error}
              </p>
            )}

            {state.notice && (
              <p role="status" className="mt-4 text-sm text-green-700">
                {state.notice}
              </p>
            )}

            {state.sessionExpired ? (
              <p className="mt-4 text-sm">
                Phiên xác thực đã hết hạn. Vui lòng bắt đầu lại.
              </p>
            ) : state.metadata ? (
              <form onSubmit={state.verify} className="mt-6 space-y-4">
                <label className="block text-sm font-medium text-gray-700">
                  Mã OTP
                  <input
                    autoFocus
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    pattern="[0-9]{6}"
                    maxLength={6}
                    value={state.otp}
                    onChange={(event) =>
                      state.setOtp(
                        event.target.value.replace(/[^0-9]/g, "").slice(0, 6),
                      )
                    }
                    disabled={state.busy}
                    required
                    className="mt-2 w-full rounded-lg border border-gray-300 px-4 py-3 text-center text-2xl tracking-widest focus:outline-blue-600"
                    aria-describedby="otp-expiry"
                  />
                </label>

                <p id="otp-expiry" className="text-sm text-gray-500">
                  {state.otpSeconds > 0
                    ? `Mã còn hiệu lực ${state.otpSeconds} giây.`
                    : "Mã đã hết hạn. Vui lòng gửi lại mã."}
                  {` Còn ${state.metadata.remainingAttempts} lần nhập.`}
                </p>

                <button
                  type="submit"
                  disabled={
                    state.busy ||
                    state.otp.length !== 6 ||
                    !state.otpSeconds ||
                    !state.metadata.remainingAttempts
                  }
                  className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white disabled:opacity-50"
                >
                  {state.busy ? "Đang xử lý..." : "Xác thực và tiếp tục"}
                </button>

                <button
                  type="button"
                  onClick={state.resend}
                  disabled={state.busy || state.resendSeconds > 0}
                  className="w-full rounded-lg border border-gray-300 py-3 text-sm font-medium disabled:opacity-50"
                >
                  {state.resendSeconds > 0
                    ? `Gửi lại sau ${state.resendSeconds} giây`
                    : "Gửi lại OTP"}
                </button>
              </form>
            ) : (
              <button className="mt-4 text-blue-600" onClick={state.reload}>
                Thử tải lại
              </button>
            )}

            <div className="mt-6 flex justify-between text-sm text-blue-600">
              <Link href="/auth/register">Đăng ký lại</Link>
              <Link href="/auth/login">Về đăng nhập</Link>
            </div>
          </>
        )}
      </section>
    </main>
  );
}
