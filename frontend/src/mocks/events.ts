import type { Event } from "@/types";

export const MOCK_EVENTS: Event[] = [
  {
    id: "1",
    title: "Spring Networking Night",
    date: "2024-06-15 18:00",
    location: "인하대학교 학생회관 201호",
    description: "Meet industry leaders and expand your network.",
    attendees: 45,
    maxCapacity: 60,
    status: "UPCOMING",
  },
  {
    id: "2",
    title: "Web Development Workshop",
    date: "2024-06-20 14:00",
    location: "하이테크센터 B101",
    description: "Learn modern web development with React and Node.js.",
    attendees: 28,
    maxCapacity: 30,
    status: "UPCOMING",
  },
  {
    id: "3",
    title: "AI/ML Study Group Kickoff",
    date: "2024-06-25 16:00",
    location: "온라인 (Zoom)",
    description: "Start your journey into AI and Machine Learning.",
    attendees: 15,
    maxCapacity: 50,
    status: "UPCOMING",
  },
];
