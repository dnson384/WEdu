import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  experimental: {
    // Match Spring's multipart request limit so a multi-image edit is not truncated by the proxy.
    proxyClientMaxBodySize: 65 * 1024 * 1024,
  },
  images: {
    qualities: [75, 100],
    remotePatterns: [
      {
        protocol: "https",
        hostname:
          "fckedu-backend-storage-static.s3.ap-southeast-2.amazonaws.com",
        port: "",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
