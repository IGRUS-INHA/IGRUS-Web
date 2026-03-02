import type { Post } from "@/types";

export const MOCK_POSTS: Post[] = [
  {
    id: "1",
    board: "notices",
    category: "Official",
    title: "2024 IGRUS Club Service Rules Update",
    author: "Admin",
    content:
      "Please review the updated rules regarding club room usage and equipment rental policies for the Spring 2024 semester.",
    date: "2024-05-20",
    likes: 12,
    comments: 2,
    isAnonymous: false,
    isQuestion: false,
  },
  {
    id: "2",
    board: "general",
    category: "Daily",
    title: "What are you all working on this weekend?",
    author: "Anonymous",
    content:
      "Just curious about everyones projects. I am currently exploring some new 3D rendering techniques using Blender.",
    date: "2024-05-21",
    likes: 45,
    comments: 23,
    isAnonymous: true,
    isQuestion: false,
  },
  {
    id: "3",
    board: "insight",
    category: "Design",
    title: "A guide to modern typography for students",
    author: "Jay Park",
    content:
      "Sharing some useful resources for understanding variable fonts and optical sizing.",
    date: "2024-05-19",
    likes: 89,
    comments: 15,
    isAnonymous: false,
    isQuestion: false,
  },
  {
    id: "4",
    board: "general",
    category: "Question",
    title: "How do I access the Figma pro workspace?",
    author: "Kim Lee",
    content:
      "I need to start the project but I am having trouble logging into the team workspace. Does anyone have the invitation link?",
    date: "2024-05-22",
    likes: 5,
    comments: 10,
    isAnonymous: false,
    isQuestion: true,
  },
];

export const FEATURED_POSTS: Post[] = [
  {
    id: "1",
    board: "notices",
    category: "Inspiration",
    title: "Spring 2024 Recruitment: Join Our Creative Odyssey",
    author: "Admin",
    content:
      "Join our team for the upcoming spring semester recruitment drive.",
    date: "2 hours ago",
    image:
      "https://images.unsplash.com/photo-1522071823991-b99c22303091?auto=format&fit=crop&q=80&w=800",
    isAnonymous: false,
    isQuestion: false,
    likes: 120,
    comments: 45,
  },
  {
    id: "2",
    board: "insight",
    category: "Showcase",
    title: "Visual Arts Exhibition: Reflecting the Future",
    author: "Sarah Jenkins",
    content:
      "An exhibition showcasing the best visual arts from our community.",
    date: "1 day ago",
    image:
      "https://images.unsplash.com/photo-1547826039-bfc35e0f1ea8?auto=format&fit=crop&q=80&w=800",
    isAnonymous: false,
    isQuestion: false,
    likes: 85,
    comments: 12,
  },
  {
    id: "3",
    board: "general",
    category: "Events",
    title: "Networking Night: Meet Industry Leaders",
    author: "Event Team",
    content:
      "A night of networking and insights with leaders from various industries.",
    date: "3 days ago",
    tag: "D-2",
    image:
      "https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&q=80&w=800",
    isAnonymous: false,
    isQuestion: false,
    likes: 210,
    comments: 38,
  },
];
