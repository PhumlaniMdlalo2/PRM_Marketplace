export const GOODS_CATEGORIES = [
  { label: 'Textbooks and study materials', value: 'Books' },
  { label: 'Electronics', value: 'Electronics' },
  { label: 'Clothing and accessories', value: 'Fashion' },
  { label: 'Home and residence essentials', value: 'Furniture' },
  { label: 'Food and drinks', value: 'Food and drinks' },
  { label: 'Health and beauty', value: 'Health and beauty' },
  { label: 'Sports and hobbies', value: 'Sports and hobbies,Bikes' },
  { label: 'Free items and giveaways', value: 'Free items and giveaways' },
];

export const SERVICE_CATEGORIES = [
  { label: 'Tutoring and academic help', value: 'Tutoring and academic help' },
  { label: 'Tech and design services', value: 'Tech and design services' },
  { label: 'Personal services', value: 'Personal services' },
  { label: 'Transport and delivery', value: 'Transport and delivery' },
  { label: 'Vendor and local business services', value: 'Vendor and local business services' },
];

export const MARKETPLACE_LISTING_CATEGORIES = [
  ...GOODS_CATEGORIES,
  ...SERVICE_CATEGORIES,
];
