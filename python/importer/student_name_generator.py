import random


class StudentNameGenerator:
    """生成导入演示用的唯一学生姓名。"""

    def __init__(self, seed: int | None = None):
        """初始化随机数生成器和已使用姓名集合。"""
        self._random = random.Random(seed)
        self._used_names: set[str] = set()

    def generate(self) -> str:
        """生成一个未重复的英文姓名。"""
        for _ in range(100):
            name = self._random_name()
            if name not in self._used_names:
                self._used_names.add(name)
                return name

        while True:
            name = f"{self._random_name()}{self._random.randint(100, 999)}"
            if name not in self._used_names:
                self._used_names.add(name)
                return name

    def _random_name(self) -> str:
        """从名字和姓氏列表中随机组合姓名。"""
        first_name = self._random.choice(FIRST_NAMES)
        last_name = self._random.choice(LAST_NAMES)
        return f"{first_name} {last_name}"


FIRST_NAMES = (
    "Aaron", "Abigail", "Adam", "Aiden", "Alex", "Alice", "Amelia", "Andrew",
    "Anna", "Anthony", "Ava", "Bella", "Benjamin", "Blake", "Brandon", "Brian",
    "Caleb", "Caroline", "Charlotte", "Chloe", "Christopher", "Claire", "Daniel",
    "David", "Dylan", "Ella", "Emily", "Emma", "Eric", "Ethan", "Evelyn",
    "Gabriel", "Grace", "Hannah", "Harper", "Henry", "Isabella", "Jack", "Jacob",
    "James", "Jason", "Jessica", "John", "Jonathan", "Joseph", "Joshua", "Julia",
    "Justin", "Katherine", "Kevin", "Laura", "Leah", "Liam", "Lily", "Logan",
    "Lucas", "Lucy", "Madison", "Matthew", "Mia", "Michael", "Natalie", "Nathan",
    "Noah", "Nora", "Olivia", "Owen", "Rachel", "Ryan", "Samuel", "Sarah",
    "Sophia", "Thomas", "Tyler", "Victoria", "William", "Zoe",
)

LAST_NAMES = (
    "Adams", "Allen", "Anderson", "Bailey", "Baker", "Barnes", "Bell", "Bennett",
    "Brooks", "Brown", "Bryant", "Butler", "Campbell", "Carter", "Clark", "Collins",
    "Cook", "Cooper", "Cox", "Davis", "Diaz", "Edwards", "Evans", "Fisher",
    "Flores", "Foster", "Garcia", "Gomez", "Gonzalez", "Gray", "Green", "Griffin",
    "Hall", "Harris", "Hayes", "Henderson", "Hill", "Howard", "Hughes", "Jackson",
    "James", "Jenkins", "Johnson", "Jones", "Kelly", "King", "Lee", "Lewis",
    "Long", "Lopez", "Martin", "Martinez", "Miller", "Mitchell", "Moore", "Morgan",
    "Murphy", "Nelson", "Parker", "Perez", "Peterson", "Phillips", "Powell", "Price",
    "Reed", "Richardson", "Rivera", "Roberts", "Robinson", "Rodriguez", "Rogers",
    "Ross", "Russell", "Sanchez", "Scott", "Smith", "Stewart", "Taylor", "Thomas",
    "Thompson", "Torres", "Turner", "Walker", "Ward", "Watson", "White", "Williams",
    "Wilson", "Wood", "Wright", "Young",
)
