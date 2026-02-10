import { Link } from "react-router-dom";
import { MapPin, Mail, Phone, Instagram, Pen } from "lucide-react";
import { useUIStore } from "@/stores";
import { cn } from "@/lib/utils";
import { CLUB_INFO, SNS_LINKS, FOOTER_LEGAL_LINKS } from "@/constants/contact";

export default function Footer() {
  const { theme } = useUIStore();
  const isDark = theme === "dark";

  return (
    <footer
      className={cn(
        "w-full border-t z-10",
        isDark ? "bg-[#1A1A1A] border-white/5" : "bg-gray-50 border-gray-200",
      )}
    >
      <div className="px-s4 lg:px-s7 py-s4">
        {/* Main content - 2 columns on desktop, stack on mobile */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-s3 sm:gap-s5 max-w-7xl mx-auto">
          {/* Left: Club Info & Address */}
          <div className="space-y-s2" data-testid="club-info">
            <div>
              <h3
                className={cn(
                  "text-sm font-bold mb-s2",
                  isDark ? "text-white" : "text-black",
                )}
              >
                {CLUB_INFO.name}
              </h3>
              <p
                className={cn(
                  "hidden sm:block text-xs",
                  isDark ? "text-gray-400" : "text-gray-600",
                )}
              >
                {CLUB_INFO.fullName}
              </p>
            </div>

            {/* Address */}
            <div className="flex items-start gap-s2">
              <MapPin
                size={14}
                className={cn(
                  "mt-0.5 flex-shrink-0",
                  isDark ? "text-gray-400" : "text-gray-600",
                )}
              />
              <p
                className={cn(
                  "text-xs leading-relaxed",
                  isDark ? "text-gray-400" : "text-gray-600",
                )}
              >
                {CLUB_INFO.address}
              </p>
            </div>
          </div>

          {/* Right: Contact & SNS */}
          <div className="lg:text-right" data-testid="contact-info">
            <h4
              className={cn(
                "text-sm font-semibold mb-s2",
                isDark ? "text-white" : "text-black",
              )}
            >
              연락처
            </h4>

            <div className="space-y-s2">
              {/* Phone1 */}
              <div className="flex items-center gap-s2 lg:justify-end">
                <Phone
                  size={14}
                  className={cn(
                    "flex-shrink-0",
                    isDark ? "text-gray-400" : "text-gray-600",
                  )}
                />
                <a
                  href={`tel:${CLUB_INFO.phone1.replace(/-/g, "")}`}
                  className={cn(
                    "text-xs hover:text-primary transition-colors",
                    isDark ? "text-gray-400" : "text-gray-600",
                  )}
                >
                  {CLUB_INFO.phone1}
                </a>
              </div>

              {/* Phone2 - hidden on mobile/small */}
              <div className="hidden sm:flex items-center gap-s2 lg:justify-end">
                <Phone
                  size={14}
                  className={cn(
                    "flex-shrink-0",
                    isDark ? "text-gray-400" : "text-gray-600",
                  )}
                />
                <a
                  href={`tel:${CLUB_INFO.phone2.replace(/-/g, "")}`}
                  className={cn(
                    "text-xs hover:text-primary transition-colors",
                    isDark ? "text-gray-400" : "text-gray-600",
                  )}
                >
                  {CLUB_INFO.phone2}
                </a>
              </div>

              {/* SNS Links */}
              <div className="mt-s3 sm:mt-2">
                <h5
                  className={cn(
                    "text-sm font-semibold mb-2",
                    isDark ? "text-white" : "text-black",
                  )}
                >
                  소셜 미디어
                </h5>
                <div className="flex gap-2 lg:justify-end">
                  <a
                    href={SNS_LINKS.instagram}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label="Instagram"
                    className={cn(
                      "hover:text-primary transition-colors",
                      isDark ? "text-gray-400" : "text-gray-600",
                    )}
                  >
                    <Instagram size={16} />
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Divider */}
        <div
          className={cn(
            "my-3 max-w-7xl mx-auto border-t",
            isDark ? "border-white/5" : "border-gray-200",
          )}
        />

        {/* Bottom section: Legal links & Copyright */}
        <div
          className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-2"
          data-testid="legal-links"
        >
          {/* Copyright */}
          <p
            className={cn(
              "text-xs",
              isDark ? "text-gray-400" : "text-gray-600",
            )}
          >
            © {new Date().getFullYear()} {CLUB_INFO.name}. All rights reserved.
          </p>

          {/* Legal Links */}
          <div className="flex gap-3 text-xs">
            {FOOTER_LEGAL_LINKS.map((link, index) => (
              <span key={link.path} className="flex items-center gap-3">
                {index > 0 && (
                  <span
                    className={cn(isDark ? "text-gray-600" : "text-gray-400")}
                  >
                    |
                  </span>
                )}
                <Link
                  to={link.path}
                  className={cn(
                    "hover:text-primary transition-colors",
                    isDark ? "text-gray-400" : "text-gray-600",
                  )}
                >
                  {link.label}
                </Link>
              </span>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
