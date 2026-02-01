export const MOCK_USERS = [
  { id: '1', studentId: '20230001', name: 'Kim Min-su', status: 'Active', role: 'Member' },
  { id: '2', studentId: '20210542', name: 'Lee Ha-na', status: 'Active', role: 'Admin' },
  { id: '3', studentId: '20240122', name: 'Park Jun-ho', status: 'Suspended', role: 'Member' },
  { id: '4', studentId: '20220315', name: 'Choi Young-ji', status: 'Active', role: 'Member' },
  { id: '5', studentId: '20230789', name: 'Jung Seo-jun', status: 'Active', role: 'Member' },
  { id: '6', studentId: '20210100', name: 'Kang Mi-young', status: 'Suspended', role: 'Member' },
] as const;

export const MOCK_INQUIRIES = [
  { id: '1', title: 'Account activation issue', author: 'member@test.com', date: '2024-05-23', status: 'Pending' },
  { id: '2', title: 'Event registration failed', author: 'guest@test.com', date: '2024-05-22', status: 'In Progress' },
  { id: '3', title: 'How to change my profile picture?', author: 'newuser@test.com', date: '2024-05-21', status: 'Pending' },
  { id: '4', title: 'Website feedback', author: 'user@test.com', date: '2024-05-20', status: 'Answered' },
] as const;

export const MOCK_ASSOCIATES = [
  { id: '20249999', name: 'New Student 1', date: '2024-05-25', intro: 'Hi, I love coding!' },
  { id: '20248888', name: 'New Student 2', date: '2024-05-24', intro: 'Interested in UI/UX.' },
] as const;

export const MOCK_SCRAPS = [
  { id: '1', title: 'Scrap 1', user: 'user1@test.com', date: '2024-05-23', category: 'Tech' },
  { id: '2', title: 'Scrap 2', user: 'user2@test.com', date: '2024-05-22', category: 'Design' },
  { id: '3', title: 'Scrap 3', user: 'user3@test.com', date: '2024-05-21', category: 'Tech' },
  { id: '4', title: 'Scrap 4', user: 'user4@test.com', date: '2024-05-20', category: 'Marketing' },
  { id: '5', title: 'Scrap 5', user: 'user5@test.com', date: '2024-05-19', category: 'Design' },
  { id: '6', title: 'Scrap 6', user: 'user6@test.com', date: '2024-05-18', category: 'Tech' },
  { id: '7', title: 'Scrap 7', user: 'user7@test.com', date: '2024-05-17', category: 'Business' },
  { id: '8', title: 'Scrap 8', user: 'user8@test.com', date: '2024-05-16', category: 'Design' },
  { id: '9', title: 'Scrap 9', user: 'user9@test.com', date: '2024-05-15', category: 'Tech' },
  { id: '10', title: 'Scrap 10', user: 'user10@test.com', date: '2024-05-14', category: 'Marketing' },
] as const;
